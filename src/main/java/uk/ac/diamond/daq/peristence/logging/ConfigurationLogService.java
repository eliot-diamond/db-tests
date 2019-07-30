package uk.ac.diamond.daq.peristence.logging;

import uk.ac.diamond.daq.peristence.data.PersistableItem;
import uk.ac.diamond.daq.peristence.service.PersistenceException;

import java.util.List;

public interface ConfigurationLogService {

    List<LogToken> listConfigurationChanges() throws PersistenceException;

    LogToken logConfigurationChanges(PersistableItem item, String description);
}
