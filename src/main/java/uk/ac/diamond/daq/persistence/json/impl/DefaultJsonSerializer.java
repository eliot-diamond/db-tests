package uk.ac.diamond.daq.persistence.json.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.json.JsonSerializer;
import uk.ac.diamond.daq.persistence.json.PersistenceIdDeserializer;
import uk.ac.diamond.daq.persistence.json.PersistenceIdSerializer;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.PersistenceService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DefaultJsonSerializer implements JsonSerializer {
    private static final Logger log = LoggerFactory.getLogger(JsonSerializer.class);

    private ObjectMapper objectMapper;

    private class NullPersistableItem extends PersistableItem {
    }

    public DefaultJsonSerializer() {
        objectMapper = new ObjectMapper();

        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(new PersistenceIdSerializer());
        simpleModule.addDeserializer(BigInteger.class, new PersistenceIdDeserializer());
        objectMapper.registerModule(simpleModule);
    }

    private PersistableItem getPersistableItemFromJsonNode(JsonNode node, PersistenceService persistenceService)
            throws PersistenceException {
        if (node instanceof ObjectNode) {
            ObjectNode childObjectNode = (ObjectNode) node;
            JsonNode classNode = childObjectNode.get("class");
            JsonNode idNode = childObjectNode.get("id");
            if (classNode != null && idNode != null) {
                try {
                    Class<?> clazz = Class.forName(classNode.asText());
                    if (PersistableItem.class.isAssignableFrom(clazz)) {
                        long id = idNode.longValue();
                        return persistenceService.get(id, (Class<? extends PersistableItem>) clazz);
                    }
                } catch (ClassNotFoundException e) {
                    //Ignore because we are going to return as an error...
                }
            }
        }
        return null;
    }

    private void deserializeArray(List<ObjectPath> objectPaths, ObjectPath objectPath, ArrayNode arrayNode,
                                  PersistenceService persistenceService) throws PersistenceException {
        Iterator<JsonNode> iterator = arrayNode.iterator();

        List<Integer> toRemove = new ArrayList<>();

        for (int i = 0; iterator.hasNext(); i++) {
            JsonNode arrayItemNode = iterator.next();
            if (arrayItemNode instanceof ObjectNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, i);
                PersistableItem fieldItem = getPersistableItemFromJsonNode(arrayItemNode, persistenceService);
                if (fieldItem != null) {
                    childObjectPath.setItem(fieldItem);
                    objectPaths.add(childObjectPath);
                    toRemove.add(i);
                } else {
                    deserializeObject(objectPaths, childObjectPath, (ObjectNode) arrayItemNode, persistenceService);
                }
            } else if (arrayItemNode instanceof ArrayNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, i);
                deserializeArray(objectPaths, childObjectPath, (ArrayNode) arrayItemNode, persistenceService);
            }
        }

        toRemove.forEach((i) -> {
            arrayNode.remove(i);
            arrayNode.insertNull(i);
        });
    }

    private void deserializeObject(List<ObjectPath> objectPaths, ObjectPath objectPath, ObjectNode objectNode,
                                   PersistenceService persistenceService) throws PersistenceException {
        Iterator<Entry<String, JsonNode>> iterator = objectNode.fields();
        while (iterator.hasNext()) {
            Entry<String, JsonNode> entry = iterator.next();
            JsonNode childNode = entry.getValue();

            if (childNode instanceof ObjectNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, entry.getKey());
                PersistableItem fieldItem = getPersistableItemFromJsonNode(entry.getValue(), persistenceService);
                if (fieldItem != null) {
                    childObjectPath.setItem(fieldItem);
                    objectPaths.add(childObjectPath);
                    JsonNode fieldNode = JsonNodeFactory.instance.nullNode();
                    objectNode.put(entry.getKey(), fieldNode);
                } else {
                    deserializeObject(objectPaths, childObjectPath, (ObjectNode) entry.getValue(), persistenceService);
                }
            } else if (childNode instanceof ArrayNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, entry.getKey());
                deserializeArray(objectPaths, childObjectPath, (ArrayNode) childNode, persistenceService);
            }
        }
    }

    @Override
    public <T extends PersistableItem> T deserialize(String json, Class<T> clazz, PersistenceService persistenceService)
            throws PersistenceException {
        try {
            List<ObjectPath> objectPaths = new ArrayList<>();
            ObjectPath objectPath = new ObjectPath();
            ObjectNode baseNode = (ObjectNode) objectMapper.readTree(json);
            deserializeObject(objectPaths, objectPath, baseNode, persistenceService);

            T item = objectMapper.treeToValue(baseNode, clazz);

            for (ObjectPath foundObjectPath : objectPaths) {
                foundObjectPath.applyTo(item);
            }
            return item;
        } catch (IOException | IllegalAccessException e) {
            throw new PersistenceException("Unable to deserialize item " + json + " class " + clazz, e);
        }
    }

    static Field findFieldInClass(Class<?> clazz, String fieldName) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        Class<?> parent = clazz.getSuperclass();
        if (parent != null && parent != Object.class) {
            return findFieldInClass(parent, fieldName);
        }
        return null;
    }

    private void serializeMap(ObjectNode objectNode, Map<Object, Object> map, PersistenceService persistenceService)
            throws PersistenceException, IllegalAccessException {
        for (Entry<Object, Object> entry : map.entrySet()) {
            JsonNode node = objectNode.get(entry.getKey().toString());
            if (node == null) {
                throw new PersistenceException("No value found for map item: " + entry.getKey().toString());
            }
            if (node instanceof ObjectNode) {
                if (entry.getValue() instanceof PersistableItem) {
                    PersistableItem fieldItem = (PersistableItem) entry.getValue();
                    persistenceService.save(fieldItem);
                    ObjectNode newNode = JsonNodeFactory.instance.objectNode();
                    newNode.put("id", fieldItem.getId());
                    newNode.put("version", fieldItem.getVersion());
                    newNode.put("class", fieldItem.getClass().getCanonicalName());
                    objectNode.put(entry.getKey().toString(), newNode);
                } else if (entry.getValue() instanceof Map) {
                    serializeMap((ObjectNode) node, (Map) entry.getValue(), persistenceService);
                } else {
                    serializeObject((ObjectNode) node, entry.getValue(), persistenceService);
                }
            } else if (node instanceof ArrayNode) {
                serializeArray((ArrayNode) node, (List<Object>) entry.getValue(), persistenceService);
            }
        }
    }

    private void serializeArray(ArrayNode arrayNode, List<Object> array, PersistenceService persistenceService)
            throws PersistenceException, IllegalAccessException {
        Iterator<JsonNode> iterator = arrayNode.iterator();

        List<JsonNode> nodes = new ArrayList<>();

        for (int i = 0; iterator.hasNext(); i++) {
            JsonNode arrayItemNode = iterator.next();
            Object item = array.get(i);
            if (item == null) {
                nodes.add(JsonNodeFactory.instance.nullNode());
            } else if (PersistableItem.class.isAssignableFrom(item.getClass())) {
                iterator.remove();
                PersistableItem fieldItem = (PersistableItem) item;
                persistenceService.save(fieldItem);
                ObjectNode newNode = JsonNodeFactory.instance.objectNode();
                newNode.put("id", fieldItem.getId());
                newNode.put("version", fieldItem.getVersion());
                newNode.put("class", fieldItem.getClass().getCanonicalName());
                nodes.add(newNode);
            } else if (item instanceof Map) {
                serializeMap((ObjectNode) arrayItemNode, (Map) item, persistenceService);
            } else {
                serializeObject((ObjectNode) arrayItemNode, item, persistenceService);
            }
        }

        if (!nodes.isEmpty()) {
            arrayNode.removeAll();
            arrayNode.addAll(nodes);
        }
    }

    @SuppressWarnings("unchecked")
    private void serializeObject(ObjectNode objectNode, Object parent, PersistenceService persistenceService)
            throws PersistenceException, IllegalAccessException {
        Iterator<Entry<String, JsonNode>> iterator = objectNode.fields();
        while (iterator.hasNext()) {
            Entry<String, JsonNode> entry = iterator.next();
            Field field = findFieldInClass(parent.getClass(), entry.getKey());
            if (field == null) {
                throw new PersistenceException("Cannot find field " + entry.getKey() + " in " + parent.getClass());
            }
            if (PersistableItem.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                PersistableItem fieldItem = (PersistableItem) field.get(parent);
                if (fieldItem == null) {
                    objectNode.putNull(field.getName());
                } else {
                    persistenceService.save(fieldItem);
                    ObjectNode newNode = objectNode.putObject(field.getName());
                    newNode.put("id", fieldItem.getId());
                    newNode.put("version", fieldItem.getVersion());
                    newNode.put("class", fieldItem.getClass().getCanonicalName());
                }
            } else if (entry.getValue() instanceof ObjectNode) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    serializeMap((ObjectNode) entry.getValue(), (Map) field.get(parent), persistenceService);
                } else {
                    serializeObject((ObjectNode) entry.getValue(), field.get(parent), persistenceService);
                }
            } else if (entry.getValue() instanceof ArrayNode) {
                field.setAccessible(true);
                serializeArray((ArrayNode) entry.getValue(), (List<Object>) field.get(parent), persistenceService);
            }
        }
    }

    @Override
    public String serialize(PersistableItem item, PersistenceService persistenceService) throws PersistenceException {
        try {
            ObjectNode baseNode = objectMapper.valueToTree(item);
            serializeObject(baseNode, item, persistenceService);

            if (log.isTraceEnabled()) {
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(baseNode);
                log.trace("JSON for item {} class {}\n{}", item.getId(), item.getClass(), json);
                return json;
            }
            return objectMapper.writeValueAsString(baseNode);
        } catch (JsonProcessingException | IllegalAccessException e) {
            throw new PersistenceException("Unable to serialize item " + item.getId() + " class " + item.getClass(), e);
        }
    }
}
