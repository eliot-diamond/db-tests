package uk.ac.diamond.daq.persistence.service;

import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

public class Neo4jSessionFactory {

    static Configuration configuration = new Configuration.Builder().uri("bolt://neo4j:999@localhost").build();

    static SessionFactory sessionFactory = new SessionFactory(configuration, "uk.ac.diamond.daq.persistence.data");

    static Neo4jSessionFactory factory = new Neo4jSessionFactory();

    private Neo4jSessionFactory() {
    }

    public static Neo4jSessionFactory getInstance() {
        return factory;
    }

    public Session getNeo4jSession() {
        return sessionFactory.openSession();
    }
}