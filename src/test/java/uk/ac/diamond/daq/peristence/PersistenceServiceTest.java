package uk.ac.diamond.daq.peristence;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.peristence.data.DiffractionScan;
import uk.ac.diamond.daq.peristence.data.Scan;
import uk.ac.diamond.daq.peristence.data.TomographyScan;
import uk.ac.diamond.daq.peristence.service.*;
import uk.ac.diamond.daq.peristence.service.impl.InMemoryPersistenceService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class PersistenceServiceTest {
    private static final Logger log = LoggerFactory.getLogger(PersistenceServiceTest.class);


    @Test
    public void get() throws PersistenceException {
        PersistenceService persistenceService = new InMemoryPersistenceService();
        persistenceService.save(new DiffractionScan(1, "Diff 1", 0, 0, 10, 10));
        persistenceService.save(new TomographyScan(2, "Tomo Scan 1", 100, 360.0));

        SearchResults<Scan> searchResults = persistenceService.get(Scan.class);
        Set<SearchResultHeading> headings = searchResults.getHeadings();
        assertEquals("Not all headings created", 6, headings.size());
        for (SearchResultHeading heading : headings) {
            log.info ("Found heading with title : {}, priority: {}", heading.getTitle(), heading.getPriority());
        }

        Map<String, String> searchParameters = new HashMap<>();
        searchParameters.put(Scan.SEARCH_NAME_FIELD, "Diff 1");
        searchResults = persistenceService.get(searchParameters, Scan.class);
        for(SearchResultRow<Scan> searchResultRow : searchResults.getRows()) {
            log.info ("Found scan: {}", searchResultRow.getItem().getName());
        }
    }
}
