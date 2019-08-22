package uk.ac.diamond.daq.persistence.service.impl.neo4j;

import org.apache.commons.lang3.SerializationUtils;
import org.neo4j.ogm.session.Session;
import uk.ac.diamond.daq.persistence.data.MapHolder;
import uk.ac.diamond.daq.persistence.data.Persistable;
import uk.ac.diamond.daq.persistence.service.Neo4jSessionFactory;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.SearchResult;
import uk.ac.diamond.daq.persistence.service.impl.Neo4jUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class GenericService<T extends Persistable> implements Service<T>, Neo4jUtil {

    private static final int DEPTH_LIST = 0;
    private static final int DEPTH_ENTITY = 1;

    public static final String PREFIX = "MappedItem";

    protected Session session = Neo4jSessionFactory.getInstance().getNeo4jSession();

    public Session getSession() {
        return this.session;
    }

    @Override
    public Iterable<T> findAll() {
        return session.loadAll(getEntityType(), DEPTH_LIST);
    }

    @Override
    public T find(Long id) {
        return SerializationUtils.clone(session.load(getEntityType(), id, DEPTH_ENTITY));
    }

    @Override
    public void delete(Long id) {
        try {
            session.delete(session.load(getEntityType(), id));
        } catch (NullPointerException e) {

        }
    }

    @Override
    public void createOrUpdate(Persistable item) throws PersistenceException {
        session.save(item, DEPTH_ENTITY);
        item.setId(session.resolveGraphIdFor(item));
    }

    public abstract Class<T> getEntityType();

    public SearchResult getAll(HashMap<String, Object> searchParameters) throws PersistenceException {
        return formatSearchResults(session.query(getEntityType(), generateCypherString(searchParameters, new ArrayList<>()), searchParameters));

    }

    public void purgeDatabase() {
        session.purgeDatabase();
    }

    public T getOne(HashMap<String, Object> searchParameters) {
        return SerializationUtils.clone((T) retrievedMapping(session.queryForObject(getEntityType(), generateCypherString(searchParameters, new ArrayList<>()), searchParameters)));
    }

    public abstract Iterable<T> getForLabels(ArrayList<String> labels, HashMap<String, Object> searchParameters);

    public abstract T getOneForLabels(ArrayList<String> labels, HashMap<String, Object> searchParameters);

    public boolean isMap(Persistable item) {
        ArrayList<Field> result = getAllFields(item);
        for (Field field : result) {
            if (Map.class.isAssignableFrom(field.getType())) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Field> getAllFields(Persistable item) {
        ArrayList<Field> result = new ArrayList<>();
        Class<?> c = item.getClass();
        while (c.getSuperclass() != null){
            result.addAll(Arrays.asList(c.getDeclaredFields()));
            c = c.getSuperclass();
        }
        return result;
    }

    private Persistable retrievedMapping(Persistable retrievedItem) {
        if (!isMap(retrievedItem)) {
            return retrievedItem;
        }
        ArrayList<Field> result = getAllFields(retrievedItem);
        HashMap<String, Object> searchParameters;
        for (Field field : result) {
            if (Map.class.isAssignableFrom(field.getClass())) {
                field.setAccessible(true);
                try {

                    HashMap<String, Object> oldMap = (HashMap<String, Object>) field.get(retrievedItem);
                    HashMap<String, Object> newMap = new HashMap<>();
                    for (String key : oldMap.keySet()) {
                        if (oldMap.get(key) instanceof String && oldMap.get(key).toString().startsWith(PREFIX)) {
                            searchParameters = new HashMap<>();
                            searchParameters.put("id", new Long(newMap.get(
                                    key).toString().substring(PREFIX.length())));
                            newMap.put(key, ((MapHolder) getOne(searchParameters)).getItem());
                        } else {
                            newMap.put(key, oldMap.get(key));
                        }
                    }
                    field.set(retrievedItem, newMap);

                } catch (IllegalAccessException e) {

                }
            }
        }
        return retrievedItem;
    }
}