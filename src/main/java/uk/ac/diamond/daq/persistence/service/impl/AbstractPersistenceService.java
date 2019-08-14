package uk.ac.diamond.daq.persistence.service.impl;

import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.data.ItemContainer;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.json.JsonSerializer;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.PersistenceService;

import java.lang.reflect.Field;

public abstract class AbstractPersistenceService implements PersistenceService {
    protected enum SaveAction {doNotSave, updateCurrent, createNewInstance}

    protected JsonSerializer jsonSerializer;

    AbstractPersistenceService(JsonSerializer jsonSerializer) {
        this.jsonSerializer = jsonSerializer;
    }

    public abstract long getNextPersistenceId();

    private static SaveAction calculateChangeType(PersistableItem item, PersistableItem archivedItem, Class<?> clazz,
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

    private static SaveAction calculateChangeType(PersistableItem item, PersistableItem archivedItem) throws PersistenceException {
        SaveAction saveAction = SaveAction.doNotSave;

        Class<?> itemClass = item.getClass();
        Class<?> archivedItemClass = archivedItem.getClass();

        if (!itemClass.equals(archivedItemClass)) {
            throw new PersistenceException("Item class " + itemClass + " and archive item class " + archivedItemClass + "do not match");
        }

        return calculateChangeType(item, archivedItem, itemClass, saveAction);
    }

    protected abstract ItemContainer getActive(long persistenceId);

    protected abstract void saveToActiveItems(ItemContainer itemContainer);

    protected abstract void saveToArchiveItems(ItemContainer itemContainer);

    @Override
    public void save(PersistableItem item) throws PersistenceException {
        if (item == null) {
            throw new PersistenceException("Cannot save null");
        }
        SaveAction saveAction;
        ItemContainer itemContainer = getActive(item.getId());
        if (itemContainer != null) {
            saveAction = calculateChangeType(item, jsonSerializer.deserialize(itemContainer.getJson(), itemContainer.getItemClass(), this));
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
        String json = jsonSerializer.serialize(item, this);
        itemContainer = new ItemContainer(item, json);
        saveToActiveItems(itemContainer);
        saveToArchiveItems(itemContainer);
    }

    @Override
    public <T extends PersistableItem> T get(long persistenceId, Class<T> clazz) throws PersistenceException {
        ItemContainer itemContainer = getActive(persistenceId);
        if (itemContainer != null && clazz.isAssignableFrom(itemContainer.getItemClass())) {
            return (T) jsonSerializer.deserialize(itemContainer.getJson(), itemContainer.getItemClass(), this);
        }
        throw new PersistenceException("No item found width id of " + persistenceId);
    }

    protected abstract ItemContainer getArchivedItem(long persistenceId, long version);

    @Override
    public <T extends PersistableItem> T getArchive(long persistenceId, long version, Class<T> clazz) throws PersistenceException {
        ItemContainer itemContainer = getArchivedItem(persistenceId, version);
        if (itemContainer != null && itemContainer.getVersion() == version) {
            if (clazz.isAssignableFrom(itemContainer.getItemClass())) {
                return jsonSerializer.deserialize(itemContainer.getJson(), clazz, this);
            }
        }
        throw new PersistenceException("No item found width id of " + persistenceId + " and version " + version);
    }
}
