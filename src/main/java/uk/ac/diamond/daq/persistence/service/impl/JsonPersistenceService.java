package uk.ac.diamond.daq.persistence.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.annotation.Searchable;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.json.PersistenceIdDeserializer;
import uk.ac.diamond.daq.persistence.json.PersistenceIdSerializer;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.PersistenceService;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public abstract class JsonPersistenceService implements PersistenceService {
    private static final Logger log = LoggerFactory.getLogger(JsonPersistenceService.class);

    private ObjectMapper objectMapper;

    JsonPersistenceService() {
        objectMapper = new ObjectMapper();

        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(new PersistenceIdSerializer());
        simpleModule.addDeserializer(BigInteger.class, new PersistenceIdDeserializer());
        objectMapper.registerModule(simpleModule);
    }

    public static boolean isPersistable(Field field) {
        Annotation[] annotations = field.getAnnotations();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> clazz = annotation.getClass();
            if (Persisted.class.equals(clazz) || Listable.class.equals(clazz) || Searchable.class.equals(clazz)) {
                return true;
            }
        }
        return false;
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

    private PersistableItem getPersistableItemFromJsonNode(JsonNode node) throws PersistenceException {
        if (node instanceof ObjectNode) {
            ObjectNode childObjectNode = (ObjectNode) node;
            JsonNode classNode = childObjectNode.get("class");
            JsonNode idNode = childObjectNode.get("id");
            if (classNode != null && idNode != null) {
                try {
                    Class<?> clazz = Class.forName(classNode.asText());
                    if (PersistableItem.class.isAssignableFrom(clazz)) {
                        BigInteger id = idNode.bigIntegerValue();
                        return get(id, (Class<? extends PersistableItem>) clazz);
                    }
                } catch (ClassNotFoundException e) {
                    //Ignore because we are going to return as an error...
                }
            }
        }
        return null;
    }

    private void deserializeArray(List<ObjectPath> objectPaths, ObjectPath objectPath, ArrayNode arrayNode) throws PersistenceException {
        Iterator<JsonNode> iterator = arrayNode.iterator();

        List<Integer> toRemove = new ArrayList<>();

        for (int i = 0; iterator.hasNext(); i++) {
            JsonNode arrayItemNode = iterator.next();
            if (arrayItemNode instanceof ObjectNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, i);
                PersistableItem fieldItem = getPersistableItemFromJsonNode(arrayItemNode);
                if (fieldItem != null) {
                    childObjectPath.setItem(fieldItem);
                    objectPaths.add(childObjectPath);
                    toRemove.add(i);
                } else {
                    deserializeObject(objectPaths, childObjectPath, (ObjectNode) arrayItemNode);
                }
            } else if (arrayItemNode instanceof ArrayNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, i);
                deserializeArray(objectPaths, childObjectPath, (ArrayNode) arrayItemNode);
            }
        }

        toRemove.forEach((i) -> {
            arrayNode.remove(i);
            arrayNode.insertNull(i);
        });
    }

    private void deserializeObject(List<ObjectPath> objectPaths, ObjectPath objectPath, ObjectNode objectNode) throws PersistenceException {
        Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            JsonNode childNode = entry.getValue();

            if (childNode instanceof ObjectNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, entry.getKey());
                PersistableItem fieldItem = getPersistableItemFromJsonNode(entry.getValue());
                if (fieldItem != null) {
                    childObjectPath.setItem(fieldItem);
                    objectPaths.add(childObjectPath);
                    JsonNode fieldNode = JsonNodeFactory.instance.nullNode();
                    objectNode.put(entry.getKey(), fieldNode);
                } else {
                    deserializeObject(objectPaths, childObjectPath, (ObjectNode) entry.getValue());
                }
            } else if (childNode instanceof ArrayNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, entry.getKey());
                deserializeArray(objectPaths, childObjectPath, (ArrayNode) childNode);
            }
        }
    }

    <T extends PersistableItem> T deserialize(String json, Class<T> clazz) throws PersistenceException {
        try {
            List<ObjectPath> objectPaths = new ArrayList<>();
            ObjectPath objectPath = new ObjectPath();
            ObjectNode baseNode = (ObjectNode) objectMapper.readTree(json);
            deserializeObject(objectPaths, objectPath, baseNode);

            T item = objectMapper.treeToValue(baseNode, clazz);

            for (ObjectPath foundObjectPath : objectPaths) {
                foundObjectPath.applyTo(item);
            }
            return item;
        } catch (IOException | IllegalAccessException e) {
            throw new PersistenceException("Unable to deserialize item " + json + " class " + clazz, e);
        }
    }

    private void serializeMap(ObjectNode objectNode, Map<Object, Object> map) throws PersistenceException, IllegalAccessException {
        for (Entry<Object, Object> entry : map.entrySet()) {
            JsonNode node = objectNode.get(entry.getKey().toString());
            if (node == null) {
                throw new PersistenceException("No value found for map item: " + entry.getKey().toString());
            }
            if (node instanceof ObjectNode) {
                if (entry.getValue() instanceof PersistableItem) {
                    PersistableItem fieldItem = (PersistableItem) entry.getValue();
                    save(fieldItem);
                    ObjectNode newNode = JsonNodeFactory.instance.objectNode();
                    newNode.put("id", fieldItem.getId());
                    newNode.put("version", fieldItem.getVersion());
                    newNode.put("class", fieldItem.getClass().getCanonicalName());
                    objectNode.put(entry.getKey().toString(), newNode);
                } else if (entry.getValue() instanceof Map) {
                    serializeMap((ObjectNode) node, (Map) entry.getValue());
                } else {
                    serializeObject((ObjectNode) node, entry.getValue());
                }
            } else if (node instanceof ArrayNode) {
                serializeArray((ArrayNode) node, (List<Object>) entry.getValue());
            }
        }
    }

    private void serializeArray(ArrayNode arrayNode, List<Object> array) throws PersistenceException, IllegalAccessException {
        Iterator<JsonNode> iterator = arrayNode.iterator();

        List<JsonNode> nodes = new ArrayList<>();

        for (int i = 0; iterator.hasNext(); i++) {
            JsonNode arrayItemNode = iterator.next();
            Object item = array.get(i);
            if (PersistableItem.class.isAssignableFrom(item.getClass())) {
                iterator.remove();
                PersistableItem fieldItem = (PersistableItem) item;
                save(fieldItem);
                ObjectNode newNode = JsonNodeFactory.instance.objectNode();
                newNode.put("id", fieldItem.getId());
                newNode.put("version", fieldItem.getVersion());
                newNode.put("class", fieldItem.getClass().getCanonicalName());
                nodes.add(newNode);
            } else if (item instanceof Map) {
                serializeMap((ObjectNode) arrayItemNode, (Map) item);
            } else {
                serializeObject((ObjectNode) arrayItemNode, item);
            }
        }

        if (!nodes.isEmpty()) {
            arrayNode.removeAll();
            arrayNode.addAll(nodes);
        }
    }

    @SuppressWarnings("unchecked")
    private void serializeObject(ObjectNode objectNode, Object parent) throws PersistenceException, IllegalAccessException {
        Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            Field field = findFieldInClass(parent.getClass(), entry.getKey());
            if (field == null) {
                throw new PersistenceException("Cannot find field " + entry.getKey() + " in " + parent.getClass());
            }
            if (PersistableItem.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                PersistableItem fieldItem = (PersistableItem) field.get(parent);
                save(fieldItem);
                ObjectNode newNode = objectNode.putObject(field.getName());
                newNode.put("id", fieldItem.getId());
                newNode.put("version", fieldItem.getVersion());
                newNode.put("class", fieldItem.getClass().getCanonicalName());
            } else if (entry.getValue() instanceof ObjectNode) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    serializeMap((ObjectNode) entry.getValue(), (Map) field.get(parent));
                } else {
                    serializeObject((ObjectNode) entry.getValue(), field.get(parent));
                }
            } else if (entry.getValue() instanceof ArrayNode) {
                field.setAccessible(true);
                serializeArray((ArrayNode) entry.getValue(), (List<Object>) field.get(parent));
            }
        }
    }

    String serialize(PersistableItem item) throws PersistenceException {
        try {
            ObjectNode baseNode = objectMapper.valueToTree(item);
            serializeObject(baseNode, item);

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
