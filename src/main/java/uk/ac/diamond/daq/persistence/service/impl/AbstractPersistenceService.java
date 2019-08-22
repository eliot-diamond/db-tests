package uk.ac.diamond.daq.persistence.service.impl;

import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.data.ItemContainer;
import uk.ac.diamond.daq.persistence.data.ItemReference;
import uk.ac.diamond.daq.persistence.data.Persistable;
import uk.ac.diamond.daq.persistence.json.JsonDeserialiser;
import uk.ac.diamond.daq.persistence.json.JsonSerialisationFactory;
import uk.ac.diamond.daq.persistence.json.JsonSerialiser;
import uk.ac.diamond.daq.persistence.service.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public abstract class AbstractPersistenceService implements PersistenceService, VisitServiceListener {

    JsonSerialisationFactory jsonSerialisationFactory;
    private String currentVisitId;
    AbstractPersistenceService(JsonSerialisationFactory jsonSerialisationFactory, VisitService visitService) {
        this.currentVisitId = visitService.getCurrentVisitId();
        this.jsonSerialisationFactory = jsonSerialisationFactory;

        visitService.addListener(this);
    }

    private static SaveAction calculateChangeType(Persistable item, Persistable archivedItem, Class<?> clazz,
                                                  SaveAction saveAction) throws PersistenceException {
        try {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Persisted.class)) {
                    field.setAccessible(true);
                    if (!field.get(item).equals(field.get(archivedItem))) {
                        Persisted persisted = field.getAnnotation(Persisted.class);
                        if (persisted.key()) {
                            return SaveAction.createNewInstance;
                        }
                        saveAction = SaveAction.updateCurrent;
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new PersistenceException("Failed to decode item", e);
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            return calculateChangeType(item, archivedItem, superClass, saveAction);
        }
        return saveAction;
    }

    private static SaveAction calculateChangeType(Persistable item, Persistable archivedItem) throws PersistenceException {
        SaveAction saveAction = SaveAction.doNotSave;

        Class<?> itemClass = item.getClass();
        Class<?> archivedItemClass = archivedItem.getClass();

        if (!itemClass.equals(archivedItemClass)) {
            throw new PersistenceException("Item class " + itemClass + " and archive item class " + archivedItemClass + "do not match");
        }

        return calculateChangeType(item, archivedItem, itemClass, saveAction);
    }

    @Override
    public void currentVisitUpdated(String newVisitId) {
        currentVisitId = newVisitId;
    }

    protected abstract long getNextPersistenceId();

    protected abstract ItemContainer getActive(long persistenceId, String visitId);

    protected abstract void saveToActiveItems(ItemContainer itemContainer);

    protected abstract void saveToArchiveItems(ItemContainer itemContainer);

    public void save(Persistable item, String visitId) throws PersistenceException {
        if (item == null) {
            throw new PersistenceException("Cannot save null");
        }
        SaveAction saveAction;
        ItemContainer itemContainer = getActive(item.getId(), currentVisitId);
        if (itemContainer != null) {
            JsonDeserialiser jsonDeserialiser = jsonSerialisationFactory.getJsonDeserialiser(this, currentVisitId);
            saveAction = calculateChangeType(item, jsonDeserialiser.deserialise(itemContainer));
        } else {
            saveAction = SaveAction.createNewInstance;
        }

        if (saveAction == SaveAction.doNotSave) {
            return;
        }

        if (saveAction == SaveAction.createNewInstance) {
            item.setId(getNextPersistenceId());
            item.setVersion(0);
        } else if (saveAction == SaveAction.updateCurrent) {
            item.setVersion(itemContainer.getVersion() + 1);
        }

        delete(item.getId());
        JsonSerialiser jsonSerialiser = jsonSerialisationFactory.getJsonSerialiser(this, visitId);
        String json = jsonSerialiser.serialise(item);
        itemContainer = new ItemContainer(item, json, visitId);
        saveToActiveItems(itemContainer);
        saveToArchiveItems(itemContainer);
    }

    @Override
    public void save(Persistable item) throws PersistenceException {
        save(item, currentVisitId);
    }

    protected abstract <T extends Persistable> SearchResult get(Class<T> clazz, String visitId) throws PersistenceException;

    @Override
    public <T extends Persistable> SearchResult get(Class<T> clazz) throws PersistenceException {
        return get(clazz, currentVisitId);
    }

    protected abstract <T extends Persistable> SearchResult get(Map<String, String> searchParameters,
                                                                    Class<T> clazz, String visitId)
            throws PersistenceException;

    @Override
    public <T extends Persistable> SearchResult get(Map<String, String> searchParameters, Class<T> clazz)
            throws PersistenceException {
        return get(searchParameters, clazz, currentVisitId);
    }

    public <T extends Persistable> T get(ItemReference itemReference, JsonDeserialiser jsonDeserialiser,
                                             String visitId) throws PersistenceException {
        ItemContainer itemContainer = getActive(itemReference.getId(), visitId);
        if (itemContainer != null && itemReference.getItemClass().isAssignableFrom(itemContainer.getItemClass())) {
            return jsonDeserialiser.deserialise(itemContainer);
        }
        throw new PersistenceException("No item found width id of " + itemReference.getId() + " for visit " + visitId);
    }

    @Override
    public <T extends Persistable> T get(long persistenceId, Class<T> clazz) throws PersistenceException {
        JsonDeserialiser jsonDeserialiser = jsonSerialisationFactory.getJsonDeserialiser(this, currentVisitId);
        return get(new ItemReference(persistenceId, -1, clazz), jsonDeserialiser, currentVisitId);
    }

    public abstract List<Long> getVersions(long persistenceId, String visitId);

    @Override
    public List<Long> getVersions(long persistenceId) {
        return getVersions(persistenceId, currentVisitId);
    }

    protected abstract ItemContainer getArchivedItem(long persistenceId, long version, String visitId);

    @Override
    public <T extends Persistable> T getArchive(long persistenceId, long version, Class<T> clazz) throws PersistenceException {
        ItemContainer itemContainer = getArchivedItem(persistenceId, version, currentVisitId);
        if (itemContainer != null && itemContainer.getVersion() == version) {
            if (clazz.isAssignableFrom(itemContainer.getItemClass())) {
                JsonDeserialiser jsonDeserialiser = jsonSerialisationFactory.getJsonDeserialiser(this, currentVisitId);
                return jsonDeserialiser.deserialise(itemContainer);
            }
        }
        throw new PersistenceException("No item found width id of " + persistenceId + " and version " + version);
    }

    protected enum SaveAction {doNotSave, updateCurrent, createNewInstance}
}
