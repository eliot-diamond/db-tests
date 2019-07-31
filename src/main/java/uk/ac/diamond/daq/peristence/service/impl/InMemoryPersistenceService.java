package uk.ac.diamond.daq.peristence.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.peristence.annotation.Persisted;
import uk.ac.diamond.daq.peristence.annotation.Searchable;
import uk.ac.diamond.daq.peristence.data.PersistableItem;
import uk.ac.diamond.daq.peristence.logging.impl.ItemContainer;
import uk.ac.diamond.daq.peristence.service.PersistenceException;
import uk.ac.diamond.daq.peristence.service.PersistenceService;
import uk.ac.diamond.daq.peristence.service.SearchResults;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InMemoryPersistenceService implements PersistenceService {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(InMemoryPersistenceService.class);

    private static long persistenceId = 0;

    private Set<ItemContainer> activeItems = new HashSet<>();
    private Set<ItemContainer> archive = new HashSet<>();

    private static long getNextPersistenceId() {
        return persistenceId++;
    }

    private PersistableItem getLatest(PersistableItem item) throws PersistenceException {
        PersistableItem selectedItem = null;

        Class<?> clazz = item.getClass();

        for (ItemContainer itemContainer : activeItems) {
            if (itemContainer.getId() == item.getId()) {
                selectedItem = itemContainer.getItem();
                if (!clazz.isAssignableFrom(selectedItem.getClass())) {
                    throw new PersistenceException(String.format("Archived item with ID %d is not of requested class %s, but is class %s",
                            item.getId(), clazz.toGenericString(), selectedItem.getClass().toGenericString()));
                }
                break;
            }
        }
        return selectedItem;
    }

    private enum SaveAction {doNotSave, updateCurrent, createNewInstance}

    private static SaveAction calculateChangeType(PersistableItem item, PersistableItem archivedItem) throws PersistenceException {
        SaveAction saveAction = SaveAction.doNotSave;

        Class<?> itemClass = item.getClass();
        Class<?> archivedItemClass = archivedItem.getClass();

        if (!itemClass.equals(archivedItemClass)) {
            throw new PersistenceException("Item class " + itemClass + " and archive item class " + archivedItemClass + "do not match");
        }

        try {
            for (Field field : itemClass.getDeclaredFields()) {
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
        return saveAction;
    }

    @Override
    public void save(PersistableItem item) throws PersistenceException {
        SaveAction saveAction;
        PersistableItem storedItem = getLatest(item);
        if (storedItem != null) {
            saveAction = calculateChangeType(item, storedItem);
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
            item.incrementVersion();
        }

        activeItems.add(new ItemContainer(item));
        archive.add(new ItemContainer(item));
    }

    @Override
    public <T extends PersistableItem> SearchResults get(Class<T> clazz) throws PersistenceException {
        SearchResults result = new SearchResults();

        for (ItemContainer itemContainer : activeItems) {
            PersistableItem item = itemContainer.getItem();
            if (clazz.isAssignableFrom(item.getClass())) {
                result.addResult(clazz.cast(item));
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
    public <T extends PersistableItem> SearchResults get(Map<String, String> searchParameters, Class<T> clazz)
            throws PersistenceException {
        SearchResults results = new SearchResults();

        for (ItemContainer itemContainer : activeItems) {
            PersistableItem item = itemContainer.getItem();
            if (clazz.isAssignableFrom(item.getClass())) {
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
    public <T extends PersistableItem> T get(long persistenceId, Class<T> clazz) throws PersistenceException {
        for (ItemContainer itemContainer : activeItems) {
            if (itemContainer.getId() == persistenceId) {
                PersistableItem item = itemContainer.getItem();
                if (clazz.isAssignableFrom(item.getClass())) {
                    return clazz.cast(item);
                }
            }
        }
        throw new PersistenceException("No item found width id of " + persistenceId);
    }

    @Override
    public <T extends PersistableItem> T getArchive(long persistenceId, long version, Class<T> clazz) throws PersistenceException {
        for (ItemContainer itemContainer : archive) {
            if (itemContainer.getId() == persistenceId && itemContainer.getVersion() == version) {
                PersistableItem item = itemContainer.getItem();
                if (clazz.isAssignableFrom(item.getClass())) {
                    return clazz.cast(item);
                }
            }
        }
        throw new PersistenceException("No item found width id of " + persistenceId + " and version " + version);
    }
}
