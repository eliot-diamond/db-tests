package uk.ac.diamond.daq.persistence.service;

import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Searchable;
import uk.ac.diamond.daq.persistence.data.PersistableItem;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class SearchResult {
    private Set<SearchResultHeading> headings = new HashSet<>();
    private List<SearchResultRow> rows = new ArrayList<>();

    private void add(String title, String value, Map<SearchResultHeading, String> values) {
        SearchResultHeading heading = new SearchResultHeading(title, -1);
        headings.add(heading);
        values.put(heading, value);
    }

    private void add(Object object, Class<?> clazz, Map<SearchResultHeading, String> values)
            throws IllegalAccessException, InvocationTargetException {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Listable.class)) {
                field.setAccessible(true);
                Listable listable = field.getDeclaredAnnotation(Listable.class);
                add(listable.value(), field.get(object).toString(), values);
            } else if (field.isAnnotationPresent(Searchable.class)) {
                field.setAccessible(true);
                Searchable searchable = field.getDeclaredAnnotation(Searchable.class);
                add(searchable.value(), field.get(object).toString(), values);
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Listable.class)) {
                method.setAccessible(true);
                Listable listable = method.getDeclaredAnnotation(Listable.class);
                add(listable.value(), method.invoke(object).toString(), values);
            } else if (method.isAnnotationPresent(Searchable.class)) {
                method.setAccessible(true);
                Searchable searchable = method.getDeclaredAnnotation(Searchable.class);
                add(searchable.value(), method.invoke(object).toString(), values);
            }
        }

        Class<?> parent = clazz.getSuperclass();
        if (parent != null && !parent.equals(PersistableItem.class)) {
            add(object, parent, values);
        }
    }

    public void addResult(PersistableItem item) throws PersistenceException {
        try {
            Map<SearchResultHeading, String> values = new HashMap<>();
            add(item, item.getClass(), values);
            rows.add(new SearchResultRow(item.getId(), item.getVersion(), values));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new PersistenceException("Unable to add search results", e);
        }
    }

    public Set<SearchResultHeading> getHeadings() {
        return headings;
    }

    public List<SearchResultRow> getRows() {
        return rows;
    }
}
