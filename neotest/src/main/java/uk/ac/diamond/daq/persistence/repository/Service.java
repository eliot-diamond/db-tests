package uk.ac.diamond.daq.persistence.repository;

import uk.ac.diamond.daq.persistence.domain.PersistableItem;

interface Service<T extends PersistableItem> {

	Iterable<T> findAll();

	T find(Long id);

	void delete(Long id);

	T createOrUpdate(T object);

}
