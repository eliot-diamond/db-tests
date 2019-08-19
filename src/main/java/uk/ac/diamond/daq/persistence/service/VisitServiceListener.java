package uk.ac.diamond.daq.persistence.service;

public interface VisitServiceListener {
    void currentVisitUpdated(String newVisitId);
}
