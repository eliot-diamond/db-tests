package uk.ac.diamond.daq.persistence.service.impl.neo4j;

import uk.ac.diamond.daq.persistence.data.Persistable;
import uk.ac.diamond.daq.persistence.service.PersistenceException;

public interface Service<T extends Persistable> {

    Iterable<T> findAll();

    T find(Long id);

    void delete(Long id);

    void createOrUpdate(T object) throws PersistenceException;

}
