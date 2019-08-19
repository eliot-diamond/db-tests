package uk.ac.diamond.daq.persistence.service;

import uk.ac.diamond.daq.persistence.domain.PersistableItem;
import uk.ac.diamond.daq.persistence.repository.*;

public class AllService implements Service {

    private ScanService scanServ = new ScanService();
    private SimpleStepScanService stepScanServ = new SimpleStepScanService();
    private RotationalStepScanService rotScanServ = new RotationalStepScanService();
    private ScanMapService scanMapServ = new ScanMapService();
    private PlanService planServ = new PlanService();
    private PersistableItemService itemServ = new PersistableItemService();

    private Service[] orderedList = new Service[]{stepScanServ, rotScanServ, scanServ, scanMapServ, planServ, itemServ}

    Iterable<PersistableItem> findAll(Class ofWhatType) {
        for (Service type : orderedList) {
            if (type.getClass().equals(ofWhatType)) {
                return type.findAll();
            }
        }
        return null;
    }

    @Override
    public Iterable findAll() {
        return itemServ.findAll();
    }

    public PersistableItem find(Long id) {
        return itemServ.find(id);
    }

    public PersistableItem find(Long id, Class type) throws PersistenceException {
        return findRight(type).find(id);
    }

    public void delete(Long id) {
        itemServ.delete(id);
    }

    public void delete(Long id, Class clazz) throws PersistenceException {
        findRight(clazz).delete(id);
    }

    public PersistableItem createOrUpdate(PersistableItem item) throws PersistenceException {
        return findRight(item).createOrUpdate(item);
    }

    public PersistableItem createOrUpdate(PersistableItem item, Class clazz) throws PersistenceException {
        return findRight(clazz).createOrUpdate(item);
    }

    private Service findRight(PersistableItem item) throws PersistenceException {
        for (Service serv : orderedList) {
            if (serv.getClass().equals(item.getClass())) {
                return serv;
            }
        }
        throw new PersistenceException("Type not found");
    }

    private Service findRight(Class clazz) throws PersistenceException {
        for (Service serv : orderedList) {
            if (serv.getClass().equals(clazz)) {
                return serv;
            }
        }
        throw new PersistenceException("Type not found");
    }

}
