package uk.ac.diamond.daq.persistence.json.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.data.ItemReference;
import uk.ac.diamond.daq.persistence.data.Persistable;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.json.JsonSerialiser;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.impl.AbstractPersistenceService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultJsonSerialiser implements JsonSerialiser {
    private static final Logger log = LoggerFactory.getLogger(DefaultJsonSerialiser.class);

    private ObjectMapper objectMapper;
    private AbstractPersistenceService persistenceService;
    private String visitId;

    DefaultJsonSerialiser(AbstractPersistenceService persistenceService, String visitId) {
        objectMapper = new ObjectMapper();

        this.persistenceService = persistenceService;
        this.visitId = visitId;
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

    private void serializeMap(ObjectNode objectNode, Map<Object, Object> map)
            throws PersistenceException, IllegalAccessException {
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            JsonNode node = objectNode.get(entry.getKey().toString());
            if (node == null) {
                throw new PersistenceException("No value found for map item: " + entry.getKey().toString());
            }
            if (node instanceof ObjectNode) {
                if (entry.getValue() instanceof PersistableItem) {
                    PersistableItem fieldItem = (PersistableItem) entry.getValue();
                    persistenceService.save(fieldItem, visitId);

                    ObjectNode newNode = objectMapper.valueToTree(new ItemReference(fieldItem));
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

    private void serializeArray(ArrayNode arrayNode, List<Object> array)
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
                ObjectNode newNode = objectMapper.valueToTree(new ItemReference(fieldItem));
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
    private void serializeObject(ObjectNode objectNode, Object parent)
            throws PersistenceException, IllegalAccessException {
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
                if (fieldItem == null) {
                    objectNode.putNull(field.getName());
                } else {
                    persistenceService.save(fieldItem, visitId);
                    ObjectNode newNode = objectMapper.valueToTree(new ItemReference(fieldItem));
                    objectNode.put(field.getName(), newNode);
                }
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

    @Override
    public String serialise(Persistable item) throws PersistenceException {
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
