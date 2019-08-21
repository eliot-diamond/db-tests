package uk.ac.diamond.daq.persistence.service.impl.neo4j;

import org.neo4j.ogm.session.Session;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.Neo4jSessionFactory;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.SearchResult;
import uk.ac.diamond.daq.persistence.service.impl.Neo4jUtil;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class GenericService<T extends PersistableItem> implements Service<T>, Neo4jUtil {

    private static final int DEPTH_LIST = 0;
    private static final int DEPTH_ENTITY = 1;
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
        return session.load(getEntityType(), id, DEPTH_ENTITY);
    }

    @Override
    public void delete(Long id) {
        try {
            session.delete(session.load(getEntityType(), id));
        } catch (NullPointerException e) {

        }
    }

    @Override
    public void createOrUpdate(T object) throws PersistenceException {
        session.save(object, DEPTH_ENTITY);
    }

    public abstract Class<T> getEntityType();

    public SearchResult getAll(HashMap<String, Object> searchParameters) throws PersistenceException {
        return formatSearchResults(session.query(getEntityType(), generateCypherString(searchParameters), searchParameters));

    }

    public void purgeDatabase() {
        session.purgeDatabase();
    }

    public T getOne(HashMap<String, Object> searchParameters) {
        return session.queryForObject(getEntityType(), generateCypherString(searchParameters), searchParameters);
    }

    public abstract Iterable<T> getForLabels(ArrayList<String> labels, HashMap<String, Object> searchParameters);

    public abstract T getOneForLabels(ArrayList<String> labels, HashMap<String, Object> searchParameters);

}

