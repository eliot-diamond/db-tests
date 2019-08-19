package uk.ac.diamond.daq.persistence.service;

public interface VisitService {
    void addListener(VisitServiceListener listener);

    void removeListener(VisitServiceListener listener);

    String getCurrentVisitId();

    void setCurrentVisitId(String currentVisitId);
}
