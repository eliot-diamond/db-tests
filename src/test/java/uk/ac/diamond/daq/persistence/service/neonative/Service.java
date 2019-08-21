package uk.ac.diamond.daq.persistence.service.neonative;

import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;

public interface Service<T extends PersistableItem> {

    Iterable<T> findAll();

    T find(Long id);

    void delete(Long id);

    void createOrUpdate(T object) throws PersistenceException;

}