package uk.ac.diamond.daq.persistence.service;

import java.util.List;

public interface PersistenceManagementService {
    List<Long> getAllItems(String visitId) throws PersistenceException;

    void copy(long persistenceId, String fromVisitId, String toVisitId) throws PersistenceException;

    void copyAll(String fromVisitId, String toVisitId) throws PersistenceException;
}
