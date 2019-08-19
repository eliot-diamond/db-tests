package uk.ac.diamond.daq.persistence.service.impl;

import uk.ac.diamond.daq.persistence.service.VisitService;
import uk.ac.diamond.daq.persistence.service.VisitServiceListener;

import java.util.ArrayList;
import java.util.List;

public class TestVisitService implements VisitService {
    private List<VisitServiceListener> listeners;
    private String currentVisitId;

    public TestVisitService(String currentVisitId) {
        this.currentVisitId = currentVisitId;

        listeners = new ArrayList<>();
    }

    @Override
    public void addListener(VisitServiceListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(VisitServiceListener listener) {
        listeners.remove(listener);
    }

    @Override
    public String getCurrentVisitId() {
        return currentVisitId;
    }

    @Override
    public void setCurrentVisitId(String currentVisitId) {
        this.currentVisitId = currentVisitId;
        for (VisitServiceListener listener : listeners) {
            listener.currentVisitUpdated(currentVisitId);
        }
    }
}
