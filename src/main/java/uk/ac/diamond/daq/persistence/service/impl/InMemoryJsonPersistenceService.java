package uk.ac.diamond.daq.persistence.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Searchable;
import uk.ac.diamond.daq.persistence.data.ItemContainer;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.PersistenceService;
import uk.ac.diamond.daq.persistence.service.SearchResult;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class InMemoryJsonPersistenceService extends AbstractPersistenceService {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(InMemoryJsonPersistenceService.class);

    private static long persistenceId = 255;

    private Set<ItemContainer> activeItems = new HashSet<>();
    private Set<ItemContainer> archivedItems = new HashSet<>();

    @Override
    public long getNextPersistenceId() {
        return persistenceId++;
    }

    @Override
    protected ItemContainer getActive(long persistenceId) {
        for (ItemContainer itemContainer : activeItems) {
            if (itemContainer.getId() == persistenceId) {
                return itemContainer;
            }
        }
        return null;
    }

    @Override
    protected void saveToActiveItems(ItemContainer itemContainer) {
        activeItems.add(itemContainer);
    }

    @Override
    protected void saveToArchiveItems(ItemContainer itemContainer) {
        archivedItems.add(itemContainer);
    }

    @Override
    protected ItemContainer getArchivedItem(long persistenceId, long version) {
        for (ItemContainer itemContainer : archivedItems) {
            if (itemContainer.getId() == persistenceId && itemContainer.getVersion() == version) {
                return itemContainer;
            }
        }
        return null;
    }

    @Override
    public boolean delete(long persistenceId) {
        return activeItems.removeIf(itemContainer -> itemContainer.getId() == persistenceId);
    }

    @Override
    public <T extends PersistableItem> SearchResult get(Class<T> clazz) throws PersistenceException {
        SearchResult result = new SearchResult();

        for (ItemContainer itemContainer : activeItems) {
            if (clazz.isAssignableFrom(itemContainer.getItemClass())) {
                result.addResult(deserialize(itemContainer, new ArrayList<>()));
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
                } else if (PersistenceService.isPersistable(field)) {
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
                PersistableItem item = deserialize(itemContainer, new ArrayList<>());
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
    public List<Long> getVersions(long persistenceId) {
        List<Long> versions = new ArrayList<>();
        for (ItemContainer itemContainer : archivedItems) {
            if (itemContainer.getId() == persistenceId) {
                versions.add(itemContainer.getVersion());
            }
        }
        Collections.sort(versions);
        return versions;
    }
}
