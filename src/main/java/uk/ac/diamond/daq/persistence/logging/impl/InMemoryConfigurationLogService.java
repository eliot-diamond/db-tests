package uk.ac.diamond.daq.persistence.logging.impl;

import uk.ac.diamond.daq.persistence.data.ItemReference;
import uk.ac.diamond.daq.persistence.data.LogToken;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.logging.ConfigurationLogService;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.PersistenceService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InMemoryConfigurationLogService implements ConfigurationLogService {
    private PersistenceService persistenceService;
    private Set<LogToken> logTokens = new HashSet<>();

    public InMemoryConfigurationLogService(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public List<LogToken> listChanges(long persistenceId) throws PersistenceException {
        List<LogToken> result = new ArrayList<>();

        for (LogToken logToken : logTokens) {
            for (ItemReference itemReference : logToken.getItemReferences()) {
                if (persistenceId == itemReference.getId()) {
                    result.add(logToken);
                    break;
                }
            }
        }

        return result;
    }

    private static void addItemReferences(Object object, Class clazz, Set<ItemReference> itemReferences) throws IllegalAccessException {
        if (PersistableItem.class.isAssignableFrom(clazz)) {
            PersistableItem item = (PersistableItem) object;
            itemReferences.add(new ItemReference(item.getId(), item.getVersion(), item.getClass()));
        }
        for (Field field : clazz.getDeclaredFields()) {
            if (PersistenceService.isPersistable(field)) {
                field.setAccessible(true);
                addItemReferences(field.get(object), field.getType(), itemReferences);
            }
        }
        Class<?> parent = clazz.getSuperclass();
        if (parent != null) {
            addItemReferences(object, parent, itemReferences);
        }
    }

    @Override
    public LogToken logConfigurationChanges(PersistableItem item, String description) throws PersistenceException {
        persistenceService.save(item);
        Set<ItemReference> itemReferences = new HashSet<>();
        try {
            addItemReferences(item, item.getClass(), itemReferences);
        } catch (IllegalAccessException e) {
            throw new PersistenceException("Unable to add item references", e);
        }

        LogToken logToken = new LogToken(description, itemReferences);
        logTokens.add(logToken);
        return logToken;
    }
}
