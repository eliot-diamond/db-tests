package uk.ac.diamond.daq.persistence.service.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.PersistenceService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

abstract class JsonPersistenceService implements PersistenceService {
    private static final Logger log = LoggerFactory.getLogger(JsonPersistenceService.class);

    private static class PersistenceIdSerializer extends StdSerializer<BigInteger> {
        PersistenceIdSerializer(Class<BigInteger> t) {
            super(t);
        }

        @Override
        public void serialize(BigInteger value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString(16));
        }
    }

    private static class PersistenceIdDeserializer extends StdDeserializer<BigInteger> {
        PersistenceIdDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public BigInteger deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return new BigInteger(p.getText());
        }
    }

    private ObjectMapper objectMapper;

    JsonPersistenceService() {
        objectMapper = new ObjectMapper();

        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(new PersistenceIdSerializer(BigInteger.class));
        simpleModule.addDeserializer(BigInteger.class, new PersistenceIdDeserializer(BigInteger.class));
        objectMapper.registerModule(simpleModule);
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

    @SuppressWarnings("unchecked")
    <T extends PersistableItem> T deserialize(ItemContainer itemContainer, Class<T> clazz) throws PersistenceException {
        try {
            List<ObjectPath> objectPaths = new ArrayList<>();
            ObjectPath objectPath = new ObjectPath();
            ObjectNode baseNode = (ObjectNode) objectMapper.readTree(itemContainer.getJson());
            deserializeObject(objectPaths, objectPath, baseNode);

            T item = (T) objectMapper.treeToValue(baseNode, itemContainer.getItemClass());

            for (ObjectPath foundObjectPath : objectPaths) {
                foundObjectPath.applyTo(item);
            }
            return item;
        } catch (IOException | IllegalAccessException e) {
            throw new PersistenceException("Unable to deserialize item " + itemContainer.getId() + " class " + itemContainer.getItemClass(), e);
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

    private static void serializeArray(ArrayNode arrayNode, List<Object> array) throws PersistenceException, IllegalAccessException {
        Iterator<JsonNode> iterator = arrayNode.iterator();

        List<JsonNode> nodes = new ArrayList<>();

        for (int i = 0; iterator.hasNext(); i++) {
            JsonNode arrayItemNode = iterator.next();
            Object item = array.get(i);
            if (PersistableItem.class.isAssignableFrom(item.getClass())) {
                iterator.remove();
                PersistableItem persistableItem = (PersistableItem) item;
                ObjectNode newNode = JsonNodeFactory.instance.objectNode();
                newNode.put("id", persistableItem.getId());
                newNode.put("version", persistableItem.getVersion());
                newNode.put("class", persistableItem.getClass().getCanonicalName());
                nodes.add(newNode);
            } else {
                serializeObject(item, (ObjectNode) arrayItemNode);
            }
        }

        if (!nodes.isEmpty()) {
            arrayNode.removeAll();
            arrayNode.addAll(nodes);
        }
    }

    @SuppressWarnings("unchecked")
    private static void serializeObject(Object parent, ObjectNode objectNode) throws PersistenceException, IllegalAccessException {
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
                ObjectNode newNode = objectNode.putObject(field.getName());
                newNode.put("id", fieldItem.getId());
                newNode.put("version", fieldItem.getVersion());
                newNode.put("class", fieldItem.getClass().getCanonicalName());
            } else if (entry.getValue() instanceof ObjectNode) {
                serializeObject(field.get(parent), (ObjectNode) entry.getValue());
            } else if (entry.getValue() instanceof ArrayNode) {
                field.setAccessible(true);
                serializeArray((ArrayNode) entry.getValue(), (List<Object>) field.get(parent));
            }
        }
    }

    String serialize(PersistableItem item) throws PersistenceException {
        try {
            ObjectNode baseNode = objectMapper.valueToTree(item);
            serializeObject(item, baseNode);

            if (log.isInfoEnabled()) {
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(baseNode);
                log.info("JSON for item {} class {}\n{}", item.getId(), item.getClass(), json);
                return json;
            }
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException | IllegalAccessException e) {
            throw new PersistenceException("Unable to serialize item " + item.getId() + " class " + item.getClass(), e);
        }
    }
}
