package uk.ac.diamond.daq.persistence.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.annotation.Searchable;
import uk.ac.diamond.daq.persistence.data.ItemContainer;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.SearchResult;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;

public class InMemoryPersistenceService extends PersistenceServiceBase {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(InMemoryPersistenceService.class);

    private static long persistenceId = 0;

    private Set<ItemContainer> activeItems = new HashSet<>();
    private Set<ItemContainer> archivedItems = new HashSet<>();

    private ObjectMapper objectMapper;

    public InMemoryPersistenceService() {
        objectMapper = new ObjectMapper();
    }

    private static long getNextPersistenceId() {
        return persistenceId++;
    }

    private ItemContainer getLatest(PersistableItem item) throws PersistenceException {
        Class<?> clazz = item.getClass();

        for (ItemContainer itemContainer : activeItems) {
            if (itemContainer.getId().equals(item.getId())) {
                Class<?> itemClass = itemContainer.getItemClass();
                if (!clazz.isAssignableFrom(itemClass)) {
                    throw new PersistenceException(String.format("Archived item with ID %d is not of requested class %s, but is class %s",
                            item.getId(), clazz.toGenericString(), itemClass.toGenericString()));
                }
                return itemContainer;
            }
        }
        return null;
    }

    private enum SaveAction {doNotSave, updateCurrent, createNewInstance}

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

    private <T extends PersistableItem> T deserialize(ItemContainer itemContainer, Class<T> clazz) throws PersistenceException {
        Map<Field, PersistableItem> fieldItems = new HashMap<>();

        try {
            ObjectNode baseNode = (ObjectNode) objectMapper.readTree(itemContainer.getJson());
            Iterator<Entry<String, JsonNode>> iterator = baseNode.fields();
            while (iterator.hasNext()) {
                Entry<String, JsonNode> entry = iterator.next();
                Field field = findFieldInClass(clazz, entry.getKey());
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

            PersistableItem item = objectMapper.treeToValue(baseNode, clazz);
            for (Entry<Field, PersistableItem> entry : fieldItems.entrySet()) {
                Field field = entry.getKey();
                field.setAccessible(true);
                PersistableItem fieldItem = entry.getValue();
                field.set(item, fieldItem);
            }
            return (T) item;
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

    private String serialize(PersistableItem item) throws PersistenceException {
        try {
            if (log.isInfoEnabled()) {
                ObjectNode baseNode = objectMapper.valueToTree(item);
                Iterator<Entry<String, JsonNode>> iterator = baseNode.fields();
                while (iterator.hasNext()) {
                    Entry<String, JsonNode> entry = iterator.next();
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

    @Override
    public void save(PersistableItem item) throws PersistenceException {
        SaveAction saveAction;
        ItemContainer itemContainer = getLatest(item);
        if (itemContainer != null) {
            saveAction = calculateChangeType(item, deserialize(itemContainer, itemContainer.getItemClass()));
        } else {
            saveAction = SaveAction.createNewInstance;
        }

        if (saveAction == SaveAction.doNotSave) {
            return;
        }

        if (saveAction == SaveAction.createNewInstance) {
            item.setId(BigInteger.valueOf(getNextPersistenceId()));
            item.setVersion(0);
        } else if (saveAction == SaveAction.updateCurrent) {
            item.incrementVersion();
        }

        delete(item);
        String json = serialize(item);
        activeItems.add(new ItemContainer(item, json));
        archivedItems.add(new ItemContainer(item, json));
    }

    @Override
    public void delete(BigInteger persistenceId) {
        activeItems.removeIf(itemContainer -> itemContainer.getId().equals(persistenceId));
    }

    @Override
    public void delete(PersistableItem item) {
        delete(item.getId());
    }

    @Override
    public <T extends PersistableItem> SearchResult get(Class<T> clazz) throws PersistenceException {
        SearchResult result = new SearchResult();

        for (ItemContainer itemContainer : activeItems) {
            if (clazz.isAssignableFrom(itemContainer.getItemClass())) {
                result.addResult(deserialize(itemContainer, itemContainer.getItemClass()));
            }
        }

        return result;
    }

    private static void getSearchableValues (PersistableItem item, Class<?> clazz, Map<String, String> searchableValues)
            throws PersistenceException {
        if (clazz == null || clazz.equals(Object.class)) {
            return;
        }

        try {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Searchable.class)) {
                    Searchable searchable = field.getDeclaredAnnotation(Searchable.class);
                    searchableValues.put(searchable.value(), field.get(item).toString());
                }
            }
            for (Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(Searchable.class)) {
                    Searchable searchable = method.getDeclaredAnnotation(Searchable.class);
                    searchableValues.put(searchable.value(), method.invoke(item).toString());
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new PersistenceException("Failed to add to search results", e);
        }

        getSearchableValues(item, clazz.getSuperclass(), searchableValues);
    }

    @Override
    public <T extends PersistableItem> SearchResult get(Map<String, String> searchParameters, Class<T> clazz)
            throws PersistenceException {
        SearchResult results = new SearchResult();

        for (ItemContainer itemContainer : activeItems) {
            if (clazz.isAssignableFrom(itemContainer.getItemClass())) {
                PersistableItem item = deserialize(itemContainer, itemContainer.getItemClass());
                Map<String, String> searchableValues = new HashMap<>();
                getSearchableValues(item, item.getClass(), searchableValues);
                searchParameters.forEach((key, value) -> {
                    String itemValue = searchableValues.get(key);
                    if (itemValue != null && itemValue.equalsIgnoreCase(value)) {
                        try {
                            results.addResult(clazz.cast(item));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        }

        return results;
    }

    @Override
    public <T extends PersistableItem> T get(BigInteger persistenceId, Class<T> clazz) throws PersistenceException {
        for (ItemContainer itemContainer : activeItems) {
            if (itemContainer.getId().equals(persistenceId)) {
                if (clazz.isAssignableFrom(itemContainer.getItemClass())) {
                    return deserialize(itemContainer, clazz);
                }
            }
        }
        throw new PersistenceException("No item found width id of " + persistenceId);
    }

    @Override
    public List<Long> getVersions(BigInteger persistenceId) {
        List<Long> versions = new ArrayList<>();
        for (ItemContainer itemContainer : archivedItems) {
            if (itemContainer.getId().equals(persistenceId)) {
                versions.add(itemContainer.getVersion());
            }
        }
        Collections.sort(versions);
        return versions;
    }

    @Override
    public <T extends PersistableItem> T getArchive(BigInteger persistenceId, long version, Class<T> clazz) throws PersistenceException {
        for (ItemContainer itemContainer : archivedItems) {
            if (itemContainer.getId().equals(persistenceId) && itemContainer.getVersion() == version) {
                if (clazz.isAssignableFrom(itemContainer.getItemClass())) {
                    return deserialize(itemContainer, clazz);
                }
            }
        }
        throw new PersistenceException("No item found width id of " + persistenceId + " and version " + version);
    }
}
