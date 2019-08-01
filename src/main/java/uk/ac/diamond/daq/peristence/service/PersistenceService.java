package uk.ac.diamond.daq.peristence.service;

import uk.ac.diamond.daq.peristence.data.PersistableItem;

import java.util.List;
import java.util.Map;

public interface PersistenceService {
    void save(PersistableItem item) throws PersistenceException;

    void delete(long persistenceId);

    void delete(PersistableItem item);

    <T extends PersistableItem> SearchResult get(Class<T> clazz)
            throws PersistenceException;

    <T extends PersistableItem> SearchResult get(Map<String, String> searchParameters, Class<T> clazz)
                    throws PersistenceException;

    <T extends PersistableItem> T get(long persistenceId, Class<T> clazz) throws PersistenceException;

    List<Long> getVersions(long persistenceId);

    <T extends PersistableItem> T getArchive(long persistenceId, long version, Class<T> clazz) throws PersistenceException;
}
