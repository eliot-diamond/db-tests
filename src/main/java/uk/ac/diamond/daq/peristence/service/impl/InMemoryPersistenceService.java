package uk.ac.diamond.daq.peristence.service.impl;

import uk.ac.diamond.daq.peristence.annotation.Searchable;
import uk.ac.diamond.daq.peristence.data.PersistableItem;
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
    private Set<PersistableItem> items = new HashSet<>();

    @Override public void save (PersistableItem item) {
        items.add(item);
    }

    @Override public <T extends PersistableItem> SearchResults<T> get(Class<T> clazz)
            throws PersistenceException {
        SearchResults<T> result = new SearchResults<>();

        for (Object item : items) {
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

    @Override public <T extends PersistableItem> SearchResults<T> get(Map<String, String> searchParameters, Class<T> clazz)
            throws PersistenceException {
        SearchResults<T> results = new SearchResults<>();

        for (PersistableItem item : items) {
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
        for (PersistableItem item : items) {
            if (item.getId() == persistenceId && clazz.isAssignableFrom(item.getClass())) {
                return clazz.cast(item);
            }
        }
        throw new PersistenceException("No item found width id of " + persistenceId);
    }
}
