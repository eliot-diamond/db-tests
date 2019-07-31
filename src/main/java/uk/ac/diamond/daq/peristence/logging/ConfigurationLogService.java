package uk.ac.diamond.daq.peristence.logging;

import uk.ac.diamond.daq.peristence.data.PersistableItem;
import uk.ac.diamond.daq.peristence.service.PersistenceException;

import java.util.List;

public interface ConfigurationLogService {
    List<LogToken> listChanges(long persistenceId, Class<?> clazz) throws PersistenceException;

    List<LogToken> listChanges(PersistableItem item) throws PersistenceException;

    LogToken logConfigurationChanges(PersistableItem item, String description) throws PersistenceException;
}
