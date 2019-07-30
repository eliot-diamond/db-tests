package uk.ac.diamond.daq.peristence.logging.impl;

import uk.ac.diamond.daq.peristence.data.PersistableItem;
import uk.ac.diamond.daq.peristence.logging.ConfigurationLogService;
import uk.ac.diamond.daq.peristence.logging.LogToken;
import uk.ac.diamond.daq.peristence.service.PersistenceException;
import uk.ac.diamond.daq.peristence.service.PersistenceService;
import uk.ac.diamond.daq.peristence.service.SearchResultRow;
import uk.ac.diamond.daq.peristence.service.SearchResults;

import java.util.ArrayList;
import java.util.List;

public class InMemoryConfigurationLogService implements ConfigurationLogService {
    private PersistenceService persistenceService;

    public InMemoryConfigurationLogService(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public List<LogToken> listConfigurationChanges() throws PersistenceException {
        ArrayList<LogToken> result = new ArrayList<>();

        SearchResults<LogToken> results = persistenceService.get(LogToken.class);
        for (SearchResultRow<LogToken> row : results.getRows()) {
            result.add(row.getItem());
        }
        return result;
    }

    @Override
    public LogToken logConfigurationChanges(PersistableItem item, String description) {
        List<Long> ids = new ArrayList<>();
        ids.add(item.getId());

        LogToken logToken = new LogToken(description, ids);



        return logToken;
    }
}
