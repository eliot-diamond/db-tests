package uk.ac.diamond.daq.persistence.logging;

import org.junit.Test;
import uk.ac.diamond.daq.persistence.data.AbstractItem;
import uk.ac.diamond.daq.persistence.data.ConcreteItemA;
import uk.ac.diamond.daq.persistence.data.LogToken;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.PersistenceService;

import java.util.List;

public class ConfigurationLogServiceTest {
    ConfigurationLogService configurationLogService;
    PersistenceService persistenceService;

    @Test
    public void logFirstSimpleItem() throws PersistenceException {
        AbstractItem item = new ConcreteItemA("Item 1", 2, 3, "4");
        configurationLogService.logConfigurationChanges(item, "first ever log event");

        assertNotEquals("Item has been saved", item.getId(), PersistableItem.INVALID_ID);

        List<LogToken> logTokens = configurationLogService.listChanges(item.getId());

        assertEquals("Must contain only one log token", 1, logTokens.size());
        assertEquals("Only one item reference is stored", 1, logTokens.get(0).getItemReferences().size());
        assertEquals("Item reference has ID same as Items", item.getId(), logTokens.get(0).getItemReferences().get(0).getId());
    }
}
