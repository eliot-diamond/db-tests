package uk.ac.diamond.daq.persistence.service.impl;

import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.json.impl.DefaultJsonSerialisationFactory;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.PersistenceService;
import uk.ac.diamond.daq.persistence.service.SearchResult;
import uk.ac.diamond.daq.persistence.service.VisitService;
import uk.ac.diamond.daq.persistence.service.impl.neo4j.PersistableItemService;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Neo4jNativePersistenceService implements PersistenceService, Neo4jUtil {
    public Neo4jNativePersistenceService(DefaultJsonSerialisationFactory defaultJsonSerialisationFactory, VisitService visitService) {
    }

    private PersistableItemService service = new PersistableItemService();
    private VisitService visService;
    private long id = 255;

    public void setServices(PersistableItemService service) {
        this.service = service;
    }

    @Override
    public void save(PersistableItem item) throws PersistenceException {
        item.setId(++id);
        service.createOrUpdate(item);
    }

    @Override
    public boolean delete(long id) {
        try {
            service.find(id).getId();
        } catch (Exception e) {
            return false;
        }
        service.delete(id);
        return true;
    }

    @Override
    public <T extends PersistableItem> SearchResult get(Class<T> clazz) throws PersistenceException {
        ArrayList<String> classList = new ArrayList<>();
        return formatSearchResults(service.getForLabels(getLabels(clazz), new HashMap<String, Object>()));

    }

    @Override
    public <T extends PersistableItem> SearchResult get(Map<String, String> searchParameters, Class<T> clazz) throws PersistenceException {
        return formatSearchResults(service.getForLabels(getLabels(clazz), (HashMap) searchParameters));
    }

    @Override
    public <T extends PersistableItem> T get(long id, Class<T> clazz) throws PersistenceException {
        HashMap<String, Object> searchParameters = new HashMap<>();
        searchParameters.put("id", id);
        T retrieved = (T) service.getOneForLabels(getLabels(clazz), searchParameters);
        if (retrieved == null) {
            throw new PersistenceException("Item/Class combination not found");
        }
        return retrieved;
    }

    @Override
    public List<Long> getVersions(long id) {
        try {
            service.find(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return service.getVersions(id, visService.getCurrentVisitId(), service.getSession());
    }

    @Override
    public <T extends PersistableItem> T getArchive(long id, long version, Class<T> clazz) throws PersistenceException {
        HashMap<String, Object> searchParameters = new HashMap<>();
        searchParameters.put("id", id);
        searchParameters.put("version", version);
        T retrieved = (T) service.getOneForLabels(getLabels(clazz), searchParameters);
        if (retrieved == null) {
            throw new PersistenceException("Item/Version pair not found");
        }
        return retrieved;

    }

    public List<Long> getVersions(long id, String visitId) {
        return getVersions(id, visitId, service.getSession());
    }

    public void tearDown() {
        service.purgeDatabase();
    }

    private ArrayList<String> getLabels(Class clazz) {
        ArrayList<String> labels = new ArrayList<>();
        for (Class each : clazz.getClasses()) {
            if (each.equals(PersistableItem.class)) {
                return labels;
            }
            if (!Modifier.isAbstract(each.getModifiers())) {
                labels.add(each.getSimpleName());
            }
        }
        return labels;
    }

}
