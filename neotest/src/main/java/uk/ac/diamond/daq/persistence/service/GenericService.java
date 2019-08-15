package uk.ac.diamond.daq.persistence.service;

import uk.ac.diamond.daq.persistence.domain.PersistableItem;

public class GenericService<T extends PersistableItem> implements Servicable<T>{

	@Override
	public boolean save(T item) throws PersistenceException {
		try {
			T existingItem = get(item.getId(), item.getClass())
		}
		
		return false;
		
	}

	@Override
	public boolean delete(long persistenceId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SearchResult get(Class<T> clazz) throws PersistenceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T get(long persistenceId, Class<T> clazz) throws PersistenceException {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
