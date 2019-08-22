package uk.ac.diamond.daq.persistence.service;

import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.annotation.Searchable;
import uk.ac.diamond.daq.persistence.data.Persistable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public interface PersistenceService {
    static boolean isPersistable(Field field) {
        Annotation[] annotations = field.getAnnotations();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> clazz = annotation.getClass();
            if (Persisted.class.equals(clazz) || Listable.class.equals(clazz) || Searchable.class.equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    void save(Persistable item) throws PersistenceException;

    boolean delete(long persistenceId);

    <T extends Persistable> SearchResult get(Class<T> clazz)
            throws PersistenceException;

    <T extends Persistable> SearchResult get(Map<String, String> searchParameters, Class<T> clazz)
            throws PersistenceException;

    <T extends Persistable> T get(long persistenceId, Class<T> clazz) throws PersistenceException;

    List<Long> getVersions(long persistenceId);

    <T extends Persistable> T getArchive(long persistenceId, long version, Class<T> clazz) throws PersistenceException;
}
