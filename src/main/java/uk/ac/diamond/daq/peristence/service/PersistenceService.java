package uk.ac.diamond.daq.peristence.service;

import uk.ac.diamond.daq.peristence.data.PersistableItem;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public interface PersistenceService {
    void save(PersistableItem item);

    <T extends PersistableItem> SearchResults<T> get(Class<T> clazz)
            throws PersistenceException;

    <T extends PersistableItem> SearchResults<T> get(Map<String, String> searchParameters, Class<T> clazz)
                    throws PersistenceException;

    <T extends PersistableItem> T get(long persistenceId, Class<T> clazz) throws PersistenceException;
}
