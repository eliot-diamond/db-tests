package uk.ac.diamond.daq.persistence.service;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.data.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public abstract class PersistenceServiceTest {
    private static final Logger log = LoggerFactory.getLogger(PersistenceServiceTest.class);

    private static final String CONCRETE_ITEM_B_NAME_1 = "Tomo Scan 1";
    private static final String CONCRETE_ITEM_B_NAME_2 = "Tomo Scan 2";
    private static final String CONCRETE_ITEM_CONTAINER_NAME = "Load Trigger";

    protected PersistenceService persistenceService;

    private ConcreteItemA concreteItemA;
    private ConcreteItemB concreteItemB;

    protected void createTestData() throws PersistenceException {
        // Add a scan of each type
        concreteItemA = new ConcreteItemA("Diff 1", 2, -37);
        persistenceService.save(concreteItemA);
        concreteItemB = new ConcreteItemB(CONCRETE_ITEM_B_NAME_1, 100, 360.0);
        persistenceService.save(concreteItemB);
    }

    private static void printSearchResults(String title, SearchResult searchResult) {
        Set<SearchResultHeading> headings = searchResult.getHeadings();
        StringBuilder message = new StringBuilder();
        message.append("\n").append(title).append(":\n\nId\t|Version");
        for (SearchResultHeading heading : headings) {
            message.append("\t|").append(heading.getTitle());
        }
        for (SearchResultRow row : searchResult.getRows()) {
            message.append("\n").append(row.getPersistenceId());
            message.append("\t|").append(row.getVersion());
            for (SearchResultHeading heading : headings) {
                String value = row.getValues().get(heading);
                if (value == null) {
                    value = "";
                }
                message.append("\t|").append(value);
            }
        }
        log.info(message.toString());
    }

    @Test
    public void testDatabaseAddsId() throws PersistenceException {
        final ConcreteItemA diffractionScan = new ConcreteItemA("Diff 1", 0, 0);

        // When object is first created, its id should be set to an invalid value
        assertEquals("Invalid ID must be set", PersistableItem.INVALID_ID, diffractionScan.getId());

        // When persisted, the id should be updated to a valid value
        persistenceService.save(diffractionScan);
        assertNotEquals("Valid ID must be set", PersistableItem.INVALID_ID, diffractionScan.getId());
    }

    @Test
    public void testSearchById() throws PersistenceException {
        final ConcreteItemB tomographyScan2 = persistenceService.get(concreteItemB.getId(), ConcreteItemB.class);
        assertNotSame("Different references required", concreteItemB, tomographyScan2);
        assertEquals("Tomography Scan must equals()", concreteItemB, tomographyScan2);
    }

    @Test
    public void testSearchForAllScans() throws PersistenceException {
        // Search for all scans
        final SearchResult searchResult = persistenceService.get(AbstractItem.class);
        assertEquals("Expected 2 results", 2, searchResult.getRows().size());
        final Set<SearchResultHeading> headings = searchResult.getHeadings();
        assertEquals("Not all headings created", 4, headings.size());
        printSearchResults("All Scan Items", searchResult);
    }

    @Test
    public void testSearchUsingParameters() throws PersistenceException {
        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(AbstractItem.SEARCH_NAME_FIELD, CONCRETE_ITEM_B_NAME_1);

        final SearchResult searchResult = persistenceService.get(searchParameters, AbstractItem.class);
        assertEquals("Only one item should be found", 1, searchResult.getRows().size());

        final BigInteger id = searchResult.getRows().get(0).getPersistenceId();
        assertEquals("Wrong item found", concreteItemB.getId(), id);
        printSearchResults("Search for " + CONCRETE_ITEM_B_NAME_1, searchResult);
    }

    @Test
    public void testSaveTrigger() throws PersistenceException {
        final AbstractItemContainer abstractItemContainer = new ConcreteItemContainer("Load Trigger", concreteItemB, 78);
        persistenceService.save(abstractItemContainer);
    }

    @Test
    public void testVersionIncrementedOnEdit() throws PersistenceException {
        final long originalVersion = concreteItemB.getVersion();
        concreteItemB.setProperty1(101);
        persistenceService.save(concreteItemB);

        final List<Long> versions = persistenceService.getVersions(concreteItemB.getId());
        assertEquals("2 versions must be included", 2, versions.size());
        assertNotEquals("Version has not changed", concreteItemB.getVersion(), originalVersion);
        assertTrue("Version has not been incremented", concreteItemB.getVersion() > originalVersion);
    }

    @Test
    public void testChangingNameCreatesNewId() throws PersistenceException {
        final BigInteger originalId = concreteItemB.getId();
        concreteItemB.setName(CONCRETE_ITEM_B_NAME_2);
        persistenceService.save(concreteItemB);

        assertNotEquals("Persistence ID should have changed", concreteItemB.getId(), originalId);
        assertEquals("Version has been reset", 0, concreteItemB.getVersion());
    }

    @Test
    public void testGetReturnsNewObject() throws PersistenceException {
        final ConcreteItemB tomographyScan2 = persistenceService.get(concreteItemB.getId(), ConcreteItemB.class);
        assertNotSame("Different references required", concreteItemB, tomographyScan2);
        assertEquals("Tomography Scan must equals()", concreteItemB, tomographyScan2);
    }

    @Test
    public void testSearchForTrigger() throws PersistenceException {
        final ConcreteItemContainer trigger1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME, concreteItemB, 78);
        persistenceService.save(trigger1);

        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put("name", CONCRETE_ITEM_CONTAINER_NAME);

        final SearchResult searchResult = persistenceService.get(searchParameters, AbstractItemContainer.class);
        printSearchResults("Search for " + CONCRETE_ITEM_CONTAINER_NAME, searchResult);
        assertEquals("Only one item should be found", 1, searchResult.getRows().size());

        final BigInteger id = searchResult.getRows().get(0).getPersistenceId();
        assertEquals("Wrong item found", trigger1.getId(), id);
        final AbstractItemContainer abstractItemContainer2 = persistenceService.get(id, ConcreteItemContainer.class);
        abstractItemContainer2.execute();
    }

    @Test
    public void createPlan() throws PersistenceException {
        AbstractItemContainer abstractItemContainer1 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME + " 1", concreteItemB, 78);
        persistenceService.save(abstractItemContainer1);
        AbstractItemContainer abstractItemContainer2 = new ConcreteItemContainer(CONCRETE_ITEM_CONTAINER_NAME + " 2", concreteItemA, 99);
        persistenceService.save(abstractItemContainer2);

        String planName = "Plan 1";
        ConcreteListContainer concreteListContainer1 = new ConcreteListContainer(planName);
        concreteListContainer1.addTrigger(abstractItemContainer1);
        concreteListContainer1.addTrigger(abstractItemContainer2);
        persistenceService.save(concreteListContainer1);
        ConcreteListContainer concreteListContainer2 = persistenceService.get(concreteListContainer1.getId(), ConcreteListContainer.class);
        assertNotNull("Failed to find Plan " + concreteListContainer1.getName(), concreteListContainer2);
        assertEquals("Trigger 1", abstractItemContainer1, concreteListContainer2.getAbstractItemContainers().get(0));
        assertEquals("Trigger 1 Scan", concreteItemB, concreteListContainer2.getAbstractItemContainers().get(0).getAbstractItem());
    }
}
