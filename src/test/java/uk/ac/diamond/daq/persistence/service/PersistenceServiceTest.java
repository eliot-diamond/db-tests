package uk.ac.diamond.daq.persistence.service;

import org.junit.After;
import org.junit.Before;
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

    private static final String TOMOGRAPHY_SCAN_NAME_1 = "Tomo Scan 1";
    private static final String TOMOGRAPHY_SCAN_NAME_2 = "Tomo Scan 2";
    private static final String LOAD_TRIGGER_NAME = "Load Trigger";

    protected PersistenceService persistenceService;

    private DiffractionScan diffractionScan;
    private TomographyScan tomographyScan1;

    @Before
    public void setup() throws PersistenceException {
        beforeSetUp();

        // Add a scan of each type
        diffractionScan = new DiffractionScan("Diff 1", 0, 0, 10, 10);
        persistenceService.save(diffractionScan);
        tomographyScan1 = new TomographyScan(TOMOGRAPHY_SCAN_NAME_1, 100, 360.0);
        persistenceService.save(tomographyScan1);
        afterSetUp();
    }

    @After
    public void tearDown() {
        beforeTearDown();
        afterTearDown();
    }

    protected void beforeSetUp() {
        // by default, do nothing
    }

    protected void afterSetUp() {
        // by default, do nothing
    }

    protected void beforeTearDown() {
        // by default, do nothing
    }

    protected void afterTearDown() {
        // by default, do nothing
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
        final DiffractionScan diffractionScan = new DiffractionScan("Diff 1", 0, 0, 10, 10);

        // When object is first created, its id should be set to an invalid value
        assertEquals("Invalid ID must be set", PersistableItem.INVALID_ID, diffractionScan.getId());

        // When persisted, the id should be updated to a valid value
        persistenceService.save(diffractionScan);
        assertNotEquals("Valid ID must be set", PersistableItem.INVALID_ID, diffractionScan.getId());
    }

    @Test
    public void testSearchById() throws PersistenceException {
        final TomographyScan tomographyScan2 = persistenceService.get(tomographyScan1.getId(), TomographyScan.class);
        assertNotSame("Different references required", tomographyScan1, tomographyScan2);
        assertEquals("Tomography Scan must equals()", tomographyScan1, tomographyScan2);
    }

    @Test
    public void testSearchForAllScans() throws PersistenceException {
        // Search for all scans
        final SearchResult searchResult = persistenceService.get(Scan.class);
        assertEquals("Expected 2 results", 2, searchResult.getRows().size());
        final Set<SearchResultHeading> headings = searchResult.getHeadings();
        assertEquals("Not all headings created", 6, headings.size());
        printSearchResults("All Scan Items", searchResult);
    }

    @Test
    public void testSearchUsingParameters() throws PersistenceException {
        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(Scan.SEARCH_NAME_FIELD, TOMOGRAPHY_SCAN_NAME_1);

        final SearchResult searchResult = persistenceService.get(searchParameters, Scan.class);
        assertEquals("Only one item should be found", 1, searchResult.getRows().size());

        final BigInteger id = searchResult.getRows().get(0).getPersistenceId();
        assertEquals("Wrong item found", tomographyScan1.getId(), id);
        printSearchResults("Search for " + TOMOGRAPHY_SCAN_NAME_1, searchResult);    }

    @Test
    public void testSaveTrigger() throws PersistenceException {
        final Trigger trigger = new LoadTrigger( "Load Trigger", tomographyScan1, 78);
        persistenceService.save(trigger);
    }

    @Test
    public void testVersionIncrementedOnEdit() throws PersistenceException {
        final long originalVersion = tomographyScan1.getVersion();
        tomographyScan1.setFrames(101);
        persistenceService.save(tomographyScan1);

        final List<Long> versions = persistenceService.getVersions(tomographyScan1.getId());
        assertEquals("2 versions must be included", 2, versions.size());
        assertNotEquals("Version has not changed", tomographyScan1.getVersion(), originalVersion);
        assertTrue("Version has not been incremented", tomographyScan1.getVersion() > originalVersion);
    }

    @Test
    public void testChangingNameCreatesNewId() throws PersistenceException {
        final BigInteger originalId = tomographyScan1.getId();
        tomographyScan1.setName(TOMOGRAPHY_SCAN_NAME_2);
        persistenceService.save(tomographyScan1);

        assertNotEquals("Persistence ID should have changed", tomographyScan1.getId(), originalId);
        assertEquals("Version has been reset", 0, tomographyScan1.getVersion());
    }

    @Test
    public void testGetReturnsNewObject() throws PersistenceException {
        final TomographyScan tomographyScan2 = persistenceService.get(tomographyScan1.getId(), TomographyScan.class);
        assertNotSame("Different references required", tomographyScan1, tomographyScan2);
        assertEquals("Tomography Scan must equals()", tomographyScan1, tomographyScan2);
    }

    @Test
    public void testSearchForTrigger() throws PersistenceException {
        final LoadTrigger trigger1 = new LoadTrigger(LOAD_TRIGGER_NAME, tomographyScan1, 78);
        persistenceService.save(trigger1);

        final Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put("name", LOAD_TRIGGER_NAME);

        final SearchResult searchResult = persistenceService.get(searchParameters, Trigger.class);
        printSearchResults("Search for " + LOAD_TRIGGER_NAME, searchResult);
        assertEquals("Only one item should be found", 1, searchResult.getRows().size());

        final BigInteger id = searchResult.getRows().get(0).getPersistenceId();
        assertEquals("Wrong item found", trigger1.getId(), id);
        final Trigger trigger2 = persistenceService.get(id, LoadTrigger.class);
        trigger2.validate();
    }
}
