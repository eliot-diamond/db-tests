package uk.ac.diamond.daq.peristence.service;

import uk.ac.diamond.daq.peristence.data.PersistableItem;

import java.util.Map;

public interface PersistenceService {
    void save(PersistableItem item) throws PersistenceException;

    <T extends PersistableItem> SearchResults get(Class<T> clazz)
            throws PersistenceException;

    <T extends PersistableItem> SearchResults get(Map<String, String> searchParameters, Class<T> clazz)
                    throws PersistenceException;

    <T extends PersistableItem> T get(long persistenceId, Class<T> clazz) throws PersistenceException;

    <T extends PersistableItem> T getArchive(long persistenceId, long version, Class<T> clazz) throws PersistenceException;
}
