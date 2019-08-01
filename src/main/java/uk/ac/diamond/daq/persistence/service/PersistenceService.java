package uk.ac.diamond.daq.persistence.service;

import uk.ac.diamond.daq.persistence.data.PersistableItem;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public interface PersistenceService {
    void connect();

    void disconnect();

    void dropAll();

    void save(PersistableItem item) throws PersistenceException;

    void delete(BigInteger persistenceId);

    void delete(PersistableItem item);

    <T extends PersistableItem> SearchResult get(Class<T> clazz)
            throws PersistenceException;

    <T extends PersistableItem> SearchResult get(Map<String, String> searchParameters, Class<T> clazz)
                    throws PersistenceException;

    <T extends PersistableItem> T get(BigInteger persistenceId, Class<T> clazz) throws PersistenceException;

    List<Long> getVersions(BigInteger persistenceId);

    <T extends PersistableItem> T getArchive(BigInteger persistenceId, long version, Class<T> clazz) throws PersistenceException;
}
