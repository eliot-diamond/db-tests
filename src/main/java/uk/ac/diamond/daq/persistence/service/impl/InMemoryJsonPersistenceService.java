package uk.ac.diamond.daq.persistence.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Searchable;
import uk.ac.diamond.daq.persistence.data.ItemContainer;
import uk.ac.diamond.daq.persistence.data.Persistable;
import uk.ac.diamond.daq.persistence.json.JsonDeserialiser;
import uk.ac.diamond.daq.persistence.json.JsonSerialisationFactory;
import uk.ac.diamond.daq.persistence.service.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class InMemoryJsonPersistenceService extends AbstractPersistenceService implements PersistenceManagementService {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(InMemoryJsonPersistenceService.class);

    private long persistenceId = 255;

    private Set<ItemContainer> activeItems = new HashSet<>();
    private Set<ItemContainer> archivedItems = new HashSet<>();

    public InMemoryJsonPersistenceService(JsonSerialisationFactory jsonSerialisationFactory, VisitService visitService) {
        super(jsonSerialisationFactory, visitService);
    }

    private static void getSearchableValues(Object item, Class<?> clazz, Map<String, String> searchableValues)
            throws PersistenceException {
        if (clazz == null || clazz.equals(Object.class)) {
            return;
        }

        try {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Searchable.class)) {
                    Searchable searchable = field.getDeclaredAnnotation(Searchable.class);
                    searchableValues.put(searchable.value(), field.get(item).toString());
                } else if (PersistenceService.isPersistable(field)) {
                    getSearchableValues(field.get(item), field.getType(), searchableValues);
                }
            }
            for (Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(Searchable.class)) {
                    Searchable searchable = method.getDeclaredAnnotation(Searchable.class);
                    searchableValues.put(searchable.value(), method.invoke(item).toString());
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new PersistenceException("Failed to add to search results", e);
        }

        getSearchableValues(item, clazz.getSuperclass(), searchableValues);
    }

    @Override
    public long getNextPersistenceId() {
        return persistenceId++;
    }

    @Override
    protected ItemContainer getActive(long persistenceId, String visitId) {
        for (ItemContainer itemContainer : activeItems) {
            if (itemContainer.getId() == persistenceId && visitId.equals(itemContainer.getVisitId())) {
                return itemContainer;
            }
        }
        return null;
    }

    @Override
    protected void saveToActiveItems(ItemContainer itemContainer) {
        activeItems.add(itemContainer);
    }

    @Override
    protected void saveToArchiveItems(ItemContainer itemContainer) {
        archivedItems.add(itemContainer);
    }

    @Override
    protected ItemContainer getArchivedItem(long persistenceId, long version, String visitId) {
        for (ItemContainer itemContainer : archivedItems) {
            if (itemContainer.getId() == persistenceId && itemContainer.getVersion() == version
                    && visitId.equals(itemContainer.getVisitId())) {
                return itemContainer;
            }
        }
        return null;
    }

    @Override
    public boolean delete(long persistenceId) {
        return activeItems.removeIf(itemContainer -> itemContainer.getId() == persistenceId);
    }

    @Override
    public <T extends Persistable> SearchResult get(Class<T> clazz, String visitId) throws PersistenceException {
        SearchResult result = new SearchResult();

        JsonDeserialiser jsonDeserialiser = jsonSerialisationFactory.getJsonDeserialiser(this, visitId);
        for (ItemContainer itemContainer : activeItems) {
            if (clazz.isAssignableFrom(itemContainer.getItemClass())
                    && visitId.equals(itemContainer.getVisitId())) {
                result.addResult(jsonDeserialiser.deserialise(itemContainer));
            }
        }

        return result;
    }

    @Override
    public <T extends Persistable> SearchResult get(Map<String, String> searchParameters, Class<T> clazz,
                                                        String visitId) throws PersistenceException {
        SearchResult results = new SearchResult();

        JsonDeserialiser jsonDeserialiser = jsonSerialisationFactory.getJsonDeserialiser(this, visitId);
        for (ItemContainer itemContainer : activeItems) {
            if (clazz.isAssignableFrom(itemContainer.getItemClass()) && visitId.equals(itemContainer.getVisitId())) {
                Persistable item = jsonDeserialiser.deserialise(itemContainer);
                Map<String, String> searchableValues = new HashMap<>();
                getSearchableValues(item, item.getClass(), searchableValues);
                searchParameters.forEach((key, value) -> {
                    String itemValue = searchableValues.get(key);
                    if (itemValue != null && itemValue.equalsIgnoreCase(value)) {
                        try {
                            results.addResult(clazz.cast(item));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        }

        return results;
    }

    @Override
    public List<Long> getVersions(long persistenceId, String visitId) {
        List<Long> versions = new ArrayList<>();
        for (ItemContainer itemContainer : archivedItems) {
            if (itemContainer.getId() == persistenceId && visitId.equals(itemContainer.getVisitId())) {
                versions.add(itemContainer.getVersion());
            }
        }
        Collections.sort(versions);
        return versions;
    }

    @Override
    public List<Long> getAllItems(String visitId) {
        List<Long> persistenceIds = new ArrayList<>();

        for (ItemContainer itemContainer : activeItems) {
            if (visitId.equals(itemContainer.getVisitId())) {
                persistenceIds.add(itemContainer.getId());
            }
        }
        return persistenceIds;
    }

    @Override
    public void copy(long persistenceId, String fromVisitId, String toVisitId) throws PersistenceException {
        JsonDeserialiser jsonDeserialiser = jsonSerialisationFactory.getJsonDeserialiser(this, fromVisitId);
        ItemContainer itemContainer = getActive(persistenceId, fromVisitId);
        if (itemContainer == null) {
            throw new PersistenceException("Cannot find " + persistenceId + " in visit " + fromVisitId);
        }

        jsonDeserialiser.deserialise(itemContainer);

        for (Persistable item : jsonDeserialiser.getCache()) {
            ItemContainer fromItemContainer = getActive(item.getId(), fromVisitId);
            ItemContainer toItemContainer = getActive(item.getId(), toVisitId);

            if (fromItemContainer == null) {
                throw new PersistenceException("Item " + item.getId() + " found in visit " + fromVisitId);
            }
            ItemContainer newItemContainer;
            if (toItemContainer != null) {
                newItemContainer = new ItemContainer(toItemContainer, toItemContainer.getVersion() + 1, toVisitId);
            } else {
                newItemContainer = new ItemContainer(fromItemContainer, fromItemContainer.getVersion(), toVisitId);
            }
            activeItems.add(newItemContainer);
        }
    }

    @Override
    public void copyAll(String fromVisitId, String toVisitId) {
        for (ItemContainer fromItemContainer : activeItems) {
            if (fromVisitId.equals(fromItemContainer.getVisitId())) {
                boolean found = false;
                for (ItemContainer toItemContainer : activeItems) {
                    if (toItemContainer.getId() == fromItemContainer.getId()
                            && toItemContainer.getVisitId().equals(fromItemContainer.getVisitId())) {
                        activeItems.add(new ItemContainer(toItemContainer, toItemContainer.getVersion() + 1, toVisitId));
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    activeItems.add(new ItemContainer(fromItemContainer, fromItemContainer.getVersion(), toVisitId));
                }
            }
        }
    }
}
