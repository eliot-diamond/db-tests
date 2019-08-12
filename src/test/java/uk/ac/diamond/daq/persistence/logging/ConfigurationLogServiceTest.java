package uk.ac.diamond.daq.persistence.logging;

import org.junit.Test;
import uk.ac.diamond.daq.persistence.data.*;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.PersistenceService;

import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.*;

public class ConfigurationLogServiceTest {
    ConfigurationLogService configurationLogService;
    PersistenceService persistenceService;

    @Test
    public void logFirstSimpleItem() throws PersistenceException {
        AbstractItem item = new ConcreteItemA("Item 1", 2, 3, "A String");
        configurationLogService.logConfigurationChanges(item, "first ever log event");

        assertNotEquals("Item has been saved", PersistableItem.INVALID_ID, item.getId());

        List<LogToken> logTokens = configurationLogService.listChanges(item.getId());

        assertEquals("Must contain only one log token", 1, logTokens.size());
        assertEquals("Only one item reference is stored", 1, logTokens.get(0).getItemReferences().size());
        assertNotNull("Item reference has ID same as Items", findItemReference(logTokens.get(0), item));
    }

    private static ItemReference findItemReference(LogToken logToken, BigInteger id, long version) {
        for (ItemReference itemReference : logToken.getItemReferences()) {
            if (itemReference.getId().equals(id)) {
                if (version == -1 || logToken.getVersion() == version) {
                    return itemReference;
                }
                return null;
            }
        }
        
        return null;
    }

    private static ItemReference findItemReference(LogToken logToken, BigInteger id) {
        return findItemReference(logToken, id, -1);
    }

    private static ItemReference findItemReference(LogToken logToken, PersistableItem item) {
        return findItemReference(logToken, item.getId(), item.getVersion());
    }

    @Test
    public void logComplexItem() throws PersistenceException {
        AbstractItem item = new ConcreteItemA("Item 1", 4, 5, "A String");
        ConcreteItemContainer itemContainer = new ConcreteItemContainer("Container 1", item, 34);

        configurationLogService.logConfigurationChanges(itemContainer, "Initial changes commit");

        assertNotEquals("Container has been saved", PersistableItem.INVALID_ID, itemContainer.getId());
        assertNotEquals("Item has been saved", PersistableItem.INVALID_ID, item.getId());

        List<LogToken> logTokens;

        logTokens = configurationLogService.listChanges(itemContainer.getId());

        assertEquals("Must find a log token for container", 1, logTokens.size());
        assertEquals("2 item references are stored", 2, logTokens.get(0).getItemReferences().size());
        assertNotNull("References contain container", findItemReference(logTokens.get(0), itemContainer));
        assertNotNull("References contain item", findItemReference(logTokens.get(0), item));

        logTokens = configurationLogService.listChanges(item.getId());

        assertEquals("No log token for item is found", 0, logTokens.size());
    }
}
