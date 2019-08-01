package uk.ac.diamond.daq.persistence.logging;

import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;

import java.math.BigInteger;
import java.util.List;

public interface ConfigurationLogService {
    List<LogToken> listChanges(BigInteger persistenceId, Class<?> clazz) throws PersistenceException;

    List<LogToken> listChanges(PersistableItem item) throws PersistenceException;

    LogToken logConfigurationChanges(PersistableItem item, String description) throws PersistenceException;
}
