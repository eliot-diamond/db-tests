package uk.ac.diamond.daq.peristence.service;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.ac.diamond.daq.peristence.configuration.InMemoryConfiguration;
import uk.ac.diamond.daq.peristence.data.DiffractionScan;
import uk.ac.diamond.daq.peristence.data.PersistableItem;
import uk.ac.diamond.daq.peristence.data.Scan;
import uk.ac.diamond.daq.peristence.data.TomographyScan;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PersistenceServiceTest {
    private static final Logger log = LoggerFactory.getLogger(PersistenceServiceTest.class);

    private PersistenceService persistenceService;

    @Before
    public void setup() {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(InMemoryConfiguration.class);

        persistenceService = applicationContext.getBean("persistenceService", PersistenceService.class);
    }

    @Test
    public void get() throws PersistenceException {
        String tomographyScanName = "Tomo Scan 1";

        persistenceService.save(new DiffractionScan("Diff 1", 0, 0, 10, 10));

        TomographyScan tomographyScan1 = new TomographyScan(tomographyScanName, 100, 360.0);
        assertEquals("Invalid ID must be set", PersistableItem.INVALID_ID, tomographyScan1.getId());
        persistenceService.save(tomographyScan1);
        assertNotEquals("Valid ID must be set", PersistableItem.INVALID_ID, tomographyScan1.getId());

        TomographyScan tomographyScan2 = persistenceService.get(tomographyScan1.getId(), TomographyScan.class);
        //assertNotEquals("Different references required", tomographyScan1, tomographyScan2);
        assertEquals("Tomography Scan must equals()", tomographyScan1, tomographyScan2);

        SearchResults searchResults = persistenceService.get(Scan.class);
        Set<SearchResultHeading> headings = searchResults.getHeadings();
        assertEquals("Not all headings created", 6, headings.size());
        StringBuilder message = new StringBuilder();
        message.append("\n\nSearch Results:\n\nId");
        for (SearchResultHeading heading : headings) {
            message.append("\t|").append(heading.getTitle());
        }
        for (SearchResultRow row : searchResults.getRows()) {
            message.append("\n").append(row.getPersistenceId());
            for (SearchResultHeading heading : headings) {
                String value = row.getValues().get(heading);
                if (value == null) {
                    value = "";
                }
                message.append("\t|").append(value);
            }
        }
        log.info(message.toString());

        /*
        Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(Scan.SEARCH_NAME_FIELD, tomographyScanName);
        searchResults = persistenceService.get(searchParameters, Scan.class);
        long id;
        for(SearchResultRow searchResultRow : searchResults.getRows()) {
            id = searchResultRow.getPersistenceId();
            log.info ("Found item: {}", searchResultRow.getPersistenceId());
            tomographyScan2 = persistenceService.get(id, TomographyScan.class);
            log.info ("Tomography Scan = {} " + tomographyScan2.getName());
        }

        Trigger trigger = new LoadTrigger( "Load Trigger", tomographyScan1, 78);
        persistenceService.save(trigger);

        tomographyScan1.setFrames(101);
        persistenceService.save(tomographyScan1);
         */
    }
}
