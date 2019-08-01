package uk.ac.diamond.daq.persistence.service.impl;

import uk.ac.diamond.daq.persistence.service.PersistenceService;

/**
 * Abstract base class containing empty implementations of functions that not all services need to implement
 */
public abstract class PersistenceServiceBase implements PersistenceService {

    @Override
    public void connect() {
        // subclasses can override
    }

    @Override
    public void disconnect() {
        // subclasses can override
    }

    @Override
    public void dropAll() {
        // subclasses can override
    }
}
