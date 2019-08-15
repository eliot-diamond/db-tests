package uk.ac.diamond.daq.persistence.service;

import uk.ac.diamond.daq.persistence.domain.PersistableItem;

public interface Servicable<T extends PersistableItem> {
		
	boolean save(T item) throws PersistenceException;

	boolean delete(long persistenceId);

	SearchResult get(Class<T> clazz) throws PersistenceException;

	T get(long persistenceId, Class<T> clazz) throws PersistenceException;

}
