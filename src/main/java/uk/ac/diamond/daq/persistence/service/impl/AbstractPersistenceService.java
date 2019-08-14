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
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.data.ItemContainer;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
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

public abstract class AbstractPersistenceService implements PersistenceService {
    private static final Logger log = LoggerFactory.getLogger(AbstractPersistenceService.class);

    protected enum SaveAction {doNotSave, updateCurrent, createNewInstance}

    private ObjectMapper objectMapper;

    AbstractPersistenceService() {
        objectMapper = new ObjectMapper();

        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(new PersistenceIdSerializer());
        simpleModule.addDeserializer(BigInteger.class, new PersistenceIdDeserializer());
        objectMapper.registerModule(simpleModule);
    }

    private PersistableItem getPersistableItemFromJsonNode(JsonNode node, List<PersistableItem> cache)
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
                        return get(id, (Class<? extends PersistableItem>) clazz, cache);
                    }
                } catch (ClassNotFoundException e) {
                    throw new PersistenceException("Unable to get item " + idNode.longValue(), e);
                }
            }
        }
        return null;
    }

    private void deserializeArray(List<ObjectPath> objectPaths, ObjectPath objectPath, ArrayNode arrayNode,
                                  List<PersistableItem> cache) throws PersistenceException {
        Iterator<JsonNode> iterator = arrayNode.iterator();

        List<Integer> toRemove = new ArrayList<>();

        for (int i = 0; iterator.hasNext(); i++) {
            JsonNode arrayItemNode = iterator.next();
            if (arrayItemNode instanceof ObjectNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, i);
                PersistableItem fieldItem = getPersistableItemFromJsonNode(arrayItemNode, cache);
                if (fieldItem != null) {
                    childObjectPath.setItem(fieldItem);
                    objectPaths.add(childObjectPath);
                    toRemove.add(i);
                } else {
                    deserializeObject(objectPaths, childObjectPath, (ObjectNode) arrayItemNode, cache);
                }
            } else if (arrayItemNode instanceof ArrayNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, i);
                deserializeArray(objectPaths, childObjectPath, (ArrayNode) arrayItemNode, cache);
            }
        }

        toRemove.forEach((i) -> {
            arrayNode.remove(i);
            arrayNode.insertNull(i);
        });
    }

    private void deserializeObject(List<ObjectPath> objectPaths, ObjectPath objectPath, ObjectNode objectNode,
                                   List<PersistableItem> cache) throws PersistenceException {
        Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            JsonNode childNode = entry.getValue();

            if (childNode instanceof ObjectNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, entry.getKey());
                PersistableItem fieldItem = getPersistableItemFromJsonNode(entry.getValue(), cache);
                if (fieldItem != null) {
                    childObjectPath.setItem(fieldItem);
                    objectPaths.add(childObjectPath);
                    JsonNode fieldNode = JsonNodeFactory.instance.nullNode();
                    objectNode.put(entry.getKey(), fieldNode);
                } else {
                    deserializeObject(objectPaths, childObjectPath, (ObjectNode) entry.getValue(), cache);
                }
            } else if (childNode instanceof ArrayNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, entry.getKey());
                deserializeArray(objectPaths, childObjectPath, (ArrayNode) childNode, cache);
            }
        }
    }

    <T extends PersistableItem> T deserialize(ItemContainer itemContainer, List<PersistableItem> cache)
            throws PersistenceException {
        try {
            List<ObjectPath> objectPaths = new ArrayList<>();
            ObjectPath objectPath = new ObjectPath();
            ObjectNode baseNode = (ObjectNode) objectMapper.readTree(itemContainer.getJson());
            deserializeObject(objectPaths, objectPath, baseNode, cache);

            T item = (T) objectMapper.treeToValue(baseNode, itemContainer.getItemClass());

            for (ObjectPath foundObjectPath : objectPaths) {
                foundObjectPath.applyTo(item);
            }
            return item;
        } catch (IOException | IllegalAccessException e) {
            throw new PersistenceException("Unable to deserialize item " + itemContainer.getJson()
                    + " class " + itemContainer.getItemClass().getCanonicalName(), e);
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
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
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

    private String serialize(PersistableItem item, PersistenceService persistenceService) throws PersistenceException {
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


    public abstract long getNextPersistenceId();

    private static SaveAction calculateChangeType(PersistableItem item, PersistableItem archivedItem, Class<?> clazz,
                                                  SaveAction saveAction) throws PersistenceException {
        try {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Persisted.class)) {
                    field.setAccessible(true);
                    if (!field.get(item).equals(field.get(archivedItem))) {
                        Persisted persisted = field.getAnnotation(Persisted.class);
                        if (persisted.key()) {
                            return SaveAction.createNewInstance;
                        }
                        saveAction = SaveAction.updateCurrent;
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new PersistenceException("Failed to decode item", e);
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            return calculateChangeType(item, archivedItem, superClass, saveAction);
        }
        return saveAction;
    }

    private static SaveAction calculateChangeType(PersistableItem item, PersistableItem archivedItem) throws PersistenceException {
        SaveAction saveAction = SaveAction.doNotSave;

        Class<?> itemClass = item.getClass();
        Class<?> archivedItemClass = archivedItem.getClass();

        if (!itemClass.equals(archivedItemClass)) {
            throw new PersistenceException("Item class " + itemClass + " and archive item class " + archivedItemClass + "do not match");
        }

        return calculateChangeType(item, archivedItem, itemClass, saveAction);
    }

    protected abstract ItemContainer getActive(long persistenceId);

    protected abstract void saveToActiveItems(ItemContainer itemContainer);

    protected abstract void saveToArchiveItems(ItemContainer itemContainer);

    @Override
    public void save(PersistableItem item) throws PersistenceException {
        if (item == null) {
            throw new PersistenceException("Cannot save null");
        }
        SaveAction saveAction;
        ItemContainer itemContainer = getActive(item.getId());
        if (itemContainer != null) {
            saveAction = calculateChangeType(item, deserialize(itemContainer, new ArrayList<>()));
        } else {
            saveAction = SaveAction.createNewInstance;
        }

        if (saveAction == SaveAction.doNotSave) {
            return;
        }

        if (saveAction == SaveAction.createNewInstance) {
            item.setId(getNextPersistenceId());
            item.setVersion(0);
        } else if (saveAction == SaveAction.updateCurrent) {
            item.setVersion(itemContainer.getVersion() + 1);
        }

        delete(item.getId());
        String json = serialize(item, this);
        itemContainer = new ItemContainer(item, json);
        saveToActiveItems(itemContainer);
        saveToArchiveItems(itemContainer);
    }

    private <T extends PersistableItem> T get(long persistenceId, Class<T> clazz, List<PersistableItem> cache)
            throws PersistenceException {
        for (PersistableItem item : cache) {
            if (item.getId() == persistenceId) {
                return (T) item;
            }
        }
        ItemContainer itemContainer = getActive(persistenceId);
        if (itemContainer != null && clazz.isAssignableFrom(itemContainer.getItemClass())) {
            PersistableItem item = deserialize(itemContainer, cache);
            cache.add(item);
            return (T) item;
        }
        throw new PersistenceException("No item found width id of " + persistenceId);
    }


    @Override
    public <T extends PersistableItem> T get(long persistenceId, Class<T> clazz) throws PersistenceException {
        return get(persistenceId, clazz, new ArrayList<>());
    }

    protected abstract ItemContainer getArchivedItem(long persistenceId, long version);

    @Override
    public <T extends PersistableItem> T getArchive(long persistenceId, long version, Class<T> clazz) throws PersistenceException {
        ItemContainer itemContainer = getArchivedItem(persistenceId, version);
        if (itemContainer != null && itemContainer.getVersion() == version) {
            if (clazz.isAssignableFrom(itemContainer.getItemClass())) {
                return deserialize(itemContainer, new ArrayList<>());
            }
        }
        throw new PersistenceException("No item found width id of " + persistenceId + " and version " + version);
    }
}
