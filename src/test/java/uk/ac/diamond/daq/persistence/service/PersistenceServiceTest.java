package uk.ac.diamond.daq.persistence.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.ac.diamond.daq.persistence.configuration.InMemoryConfiguration;
import uk.ac.diamond.daq.persistence.data.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class PersistenceServiceTest {
    private static final Logger log = LoggerFactory.getLogger(PersistenceServiceTest.class);

    private PersistenceService persistenceService;

    @Before
    public void setup() {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(InMemoryConfiguration.class);

        persistenceService = applicationContext.getBean("persistenceService", PersistenceService.class);
        persistenceService.connect();
        persistenceService.dropAll();
    }

    @After
    public void tearDown() {
        persistenceService.dropAll();
        persistenceService.disconnect();
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
    public void get() throws PersistenceException {
        String tomographyScanName1 = "Tomo Scan 1";
        String tomographyScanName2 = "Tomo Scan 2";

        persistenceService.save(new DiffractionScan("Diff 1", 0, 0, 10, 10));

        TomographyScan tomographyScan1 = new TomographyScan(tomographyScanName1, 100, 360.0);
        assertEquals("Invalid ID must be set", PersistableItem.INVALID_ID, tomographyScan1.getId());
        persistenceService.save(tomographyScan1);
        assertNotEquals("Valid ID must be set", PersistableItem.INVALID_ID, tomographyScan1.getId());

        TomographyScan tomographyScan2 = persistenceService.get(tomographyScan1.getId(), TomographyScan.class);
        assertNotSame("Different references required", tomographyScan1, tomographyScan2);
        assertEquals("Tomography Scan must equals()", tomographyScan1, tomographyScan2);

        SearchResult searchResult = persistenceService.get(Scan.class);
        Set<SearchResultHeading> headings = searchResult.getHeadings();
        assertEquals("Not all headings created", 6, headings.size());

        Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(Scan.SEARCH_NAME_FIELD, tomographyScanName1);
        searchResult = persistenceService.get(searchParameters, Scan.class);
        assertEquals("Only one item should be found", 1, searchResult.getRows().size());
        BigInteger id = searchResult.getRows().get(0).getPersistenceId();
        assertEquals("Wrong item found", tomographyScan1.getId(), id);
        printSearchResults("All Scan Items", searchResult);

        Trigger trigger = new LoadTrigger( "Load Trigger", tomographyScan1, 78);
        persistenceService.save(trigger);

        tomographyScan1.setFrames(101);
        persistenceService.save(tomographyScan1);

        List<Long> versions = persistenceService.getVersions(tomographyScan1.getId());
        assertEquals("2 versions must be included", 2, versions.size());
        assertNotEquals("Version has not changed", tomographyScan2.getVersion(), tomographyScan1.getVersion());

        tomographyScan1.setFrames(101);
        long previousVersion = tomographyScan1.getVersion();
        persistenceService.save(tomographyScan1);
        assertEquals("Persistence ID should not have changed", tomographyScan2.getId(), tomographyScan1.getId());
        assertEquals("Version has been incremented", previousVersion, tomographyScan1.getVersion());

        tomographyScan1.setName(tomographyScanName2);
        persistenceService.save(tomographyScan1);
        assertNotEquals("Persistence ID should have changed", tomographyScan2.getId(), tomographyScan1.getId());
        assertEquals("Version has been reset", 0, tomographyScan1.getVersion());

        tomographyScan2 = persistenceService.get(tomographyScan1.getId(), TomographyScan.class);
        assertNotSame("Different references required", tomographyScan1, tomographyScan2);
        assertEquals("Tomography Scan must equals()", tomographyScan1, tomographyScan2);
    }
}
