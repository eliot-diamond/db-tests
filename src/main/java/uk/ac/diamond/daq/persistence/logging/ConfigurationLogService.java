package uk.ac.diamond.daq.persistence.logging;

import uk.ac.diamond.daq.persistence.data.LogToken;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;

import java.util.List;

public interface ConfigurationLogService {
    List<LogToken> listChanges(long persistenceId) throws PersistenceException;

    LogToken logConfigurationChanges(PersistableItem item, String description) throws PersistenceException;
}
