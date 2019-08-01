package uk.ac.diamond.daq.peristence.service;

import uk.ac.diamond.daq.peristence.annotation.Listable;
import uk.ac.diamond.daq.peristence.data.PersistableItem;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class SearchResult {
    private Set<SearchResultHeading> headings = new HashSet<>();
    private List<SearchResultRow> rows = new ArrayList<>();

    public void addResult(PersistableItem item) throws PersistenceException {
        try {
            Map<SearchResultHeading, String> values = new HashMap<>();
            Class<?> clazz = item.getClass();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Listable.class)) {
                    Listable listable = field.getDeclaredAnnotation(Listable.class);
                    SearchResultHeading heading = new SearchResultHeading(listable.value(), listable.priority());
                    headings.add(heading);
                    values.put(heading, field.get(item).toString());
                }
            }
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Listable.class)) {
                    method.setAccessible(true);
                    Listable listable = method.getDeclaredAnnotation(Listable.class);
                    SearchResultHeading heading = new SearchResultHeading(listable.value(), listable.priority());
                    headings.add(heading);
                    values.put(heading, method.invoke(item).toString());
                }
            }

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
