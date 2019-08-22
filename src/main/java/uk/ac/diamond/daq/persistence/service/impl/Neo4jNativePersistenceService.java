package uk.ac.diamond.daq.persistence.service.impl;

import org.neo4j.ogm.annotation.NodeEntity;
import uk.ac.diamond.daq.persistence.data.ItemContainer;
import uk.ac.diamond.daq.persistence.data.MapHolder;
import uk.ac.diamond.daq.persistence.data.Persistable;
import uk.ac.diamond.daq.persistence.json.JsonSerialisationFactory;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.PersistenceService;
import uk.ac.diamond.daq.persistence.service.SearchResult;
import uk.ac.diamond.daq.persistence.service.VisitService;
import uk.ac.diamond.daq.persistence.service.impl.neo4j.PersistableItemService;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class Neo4jNativePersistenceService extends AbstractPersistenceService implements PersistenceService, Neo4jUtil {

    public Neo4jNativePersistenceService(JsonSerialisationFactory jsonSerialisationFactory, VisitService visitService) {
        super(jsonSerialisationFactory, visitService);
    }


    @Override
    protected long getNextPersistenceId() {
        return 1l;
    }

    @Override
    protected ItemContainer getActive(long persistenceId, String visitId) {
        return null;
    }

    @Override
    protected void saveToActiveItems(ItemContainer itemContainer) {

    }

    @Override
    protected void saveToArchiveItems(ItemContainer itemContainer) {

    }

    private PersistableItemService service = new PersistableItemService();
    private VisitService visService;
    private JsonSerialisationFactory jsonSerialFactory;

    public void setServices(PersistableItemService service) {
        this.service = service;
    }

    @Override
    public void save(Persistable item) throws PersistenceException {
        if (service.isMap(item)) {
            saveMap(item);
            return;
        }
        service.createOrUpdate(item);
        item.setId(service.getSession().resolveGraphIdFor(item));
    }

    private void saveMap(Persistable item) throws PersistenceException {
        ArrayList<Field> result = service.getAllFields(item);
        boolean containedMappedItem = false;
        ArrayList<MapHolder> mapHolders = new ArrayList<>();
        for (Field field : result) {
            containedMappedItem = false;
            if (Map.class.isAssignableFrom(field.getClass())) {
                field.setAccessible(true);
                try {
                    Map<String, Object> newMap = (Map) field.getType().newInstance();
                    for (String key : newMap.keySet()) {
                        if (newMap.get(key) instanceof Persistable) {
                            mapHolders.add(new MapHolder(key, (Persistable) newMap.get(key), item));
                            newMap.replace(key, service.PREFIX + ((Persistable) newMap.get(key)).getId());
                            containedMappedItem = true;
                        }
                    }
                } catch (IllegalAccessException e) {

                } catch (InstantiationException e) {

                }
            }
        }
        service.createOrUpdate(item);
        if (containedMappedItem) {
            for (MapHolder map : mapHolders) {
                service.createOrUpdate(map);
            }
        }
    }

    @Override
    protected <T extends Persistable> SearchResult get(Class<T> clazz, String visitId) throws
            PersistenceException {
        HashMap<String, Object> searchParameters = new HashMap<>();
        searchParameters.put("visitId", visitId);
        return formatSearchResults(service.getForLabels(getLabels(clazz), searchParameters));
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
    public <T extends Persistable> SearchResult get(Class<T> clazz) throws PersistenceException {
        return formatSearchResults(service.getForLabels(getLabels(clazz), new HashMap<String, Object>()));

    }

    @Override
    protected <T extends Persistable> SearchResult
    get(Map<String, String> searchParameters, Class<T> clazz, String visitId) throws PersistenceException {
        return formatSearchResults(service.getForLabels(getLabels(clazz), (HashMap) searchParameters));
    }

    @Override
    public <T extends Persistable> SearchResult get(Map<String, String> searchParameters, Class<T> clazz) throws
            PersistenceException {
        return formatSearchResults(service.getForLabels(getLabels(clazz), (HashMap) searchParameters));
    }

    @Override
    public <T extends Persistable> T get(long id, Class<T> clazz) throws PersistenceException {
        HashMap<String, Object> searchParameters = new HashMap<>();
        searchParameters.put("id", id);
        T retrieved = (T) service.getOneForLabels(getLabels(clazz), searchParameters);
        if (retrieved == null) {
            throw new PersistenceException("Item/Class combination not found");
        }
        return (retrieved);
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
    protected ItemContainer getArchivedItem(long persistenceId, long version, String visitId) {
        return null;
    }

    @Override
    public <T extends Persistable> T getArchive(long id, long version, Class<T> clazz) throws
            PersistenceException {
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
        while (clazz.getSuperclass() != null) {
            if (!Modifier.isAbstract(clazz.getModifiers()) || Arrays.asList(clazz.getAnnotations()).contains(NodeEntity.class)) {
                labels.add(clazz.getSimpleName());
            }
            clazz = clazz.getSuperclass();
        }

        return labels;
    }

}
