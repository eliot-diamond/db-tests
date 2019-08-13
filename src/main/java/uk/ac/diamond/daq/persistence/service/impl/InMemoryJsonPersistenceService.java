package uk.ac.diamond.daq.persistence.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.annotation.Searchable;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.SearchResult;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.*;

public class InMemoryJsonPersistenceService extends JsonPersistenceService {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(InMemoryJsonPersistenceService.class);

    private static long persistenceId = 255;

    private Set<ItemContainer> activeItems = new HashSet<>();
    private Set<ItemContainer> archivedItems = new HashSet<>();

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

    @Override
    public void save(PersistableItem item) throws PersistenceException {
        SaveAction saveAction;
        ItemContainer itemContainer = getLatest(item);
        if (itemContainer != null) {
            saveAction = calculateChangeType(item, deserialize(itemContainer.getJson(), itemContainer.getItemClass()));
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
                result.addResult(deserialize(itemContainer.getJson(), itemContainer.getItemClass()));
            }
        }

        return result;
    }

    private static void getSearchableValues(Object item, Class<?> clazz, Map<String, String> searchableValues)
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
                } else if (isPersistable(field)) {
                    getSearchableValues(field.get(item), field.getType(), searchableValues);
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
                PersistableItem item = deserialize(itemContainer.getJson(), itemContainer.getItemClass());
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
                    return (T) deserialize(itemContainer.getJson(), itemContainer.getItemClass());
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
                    return deserialize(itemContainer.getJson(), clazz);
                }
            }
        }
        throw new PersistenceException("No item found width id of " + persistenceId + " and version " + version);
    }
}
