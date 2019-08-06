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
import java.util.HashMap;
import java.util.Iterator;
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

    @SuppressWarnings("unchecked")
    <T extends PersistableItem> T deserialize(ItemContainer itemContainer, Class<T> clazz) throws PersistenceException {
        Map<Field, PersistableItem> fieldItems = new HashMap<>();

        try {
            ObjectNode baseNode = (ObjectNode) objectMapper.readTree(itemContainer.getJson());
            Iterator<Map.Entry<String, JsonNode>> iterator = baseNode.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                Field field = findFieldInClass(itemContainer.getItemClass(), entry.getKey());
                if (field == null) {
                    throw new PersistenceException("Cannot find field " + entry.getKey() + " in " + clazz);
                }
                if (PersistableItem.class.isAssignableFrom(field.getType())) {
                    BigInteger id = entry.getValue().get("id").bigIntegerValue();
                    Class<? extends PersistableItem> fieldClass = (Class<? extends PersistableItem>) Class.forName(entry.getValue().get("class").asText());
                    fieldItems.put(field, this.get(id, fieldClass));
                    JsonNode fieldNode = JsonNodeFactory.instance.nullNode();
                    baseNode.put(entry.getKey(), fieldNode);
                }
            }

            T item = (T) objectMapper.treeToValue(baseNode, itemContainer.getItemClass());
            for (Map.Entry<Field, PersistableItem> entry : fieldItems.entrySet()) {
                Field field = entry.getKey();
                field.setAccessible(true);
                PersistableItem fieldItem = entry.getValue();
                field.set(item, fieldItem);
            }
            return item;
        } catch (IOException | ClassNotFoundException | IllegalAccessException e) {
            throw new PersistenceException("Unable to deserialize item " + itemContainer.getId() + " class " + itemContainer.getItemClass(), e);
        }
    }

    private static Field findFieldInClass(Class<?> clazz, String fieldName) {
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

    String serialize(PersistableItem item) throws PersistenceException {
        try {
            if (log.isInfoEnabled()) {
                ObjectNode baseNode = objectMapper.valueToTree(item);
                Iterator<Map.Entry<String, JsonNode>> iterator = baseNode.fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    Field field = findFieldInClass(item.getClass(), entry.getKey());
                    if (field == null) {
                        throw new PersistenceException("Cannot find field " + entry.getKey() + " in " + item.getClass());
                    }
                    if (PersistableItem.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        PersistableItem fieldItem = (PersistableItem) field.get(item);
                        ObjectNode newNode = baseNode.putObject(entry.getKey());
                        newNode.put("id", fieldItem.getId());
                        newNode.put("version", fieldItem.getVersion());
                        newNode.put("class", fieldItem.getClass().getCanonicalName());
                    }
                }

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
