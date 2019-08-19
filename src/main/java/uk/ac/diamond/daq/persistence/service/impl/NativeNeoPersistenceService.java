//package uk.ac.diamond.daq.persistence.service.impl;
//
//import uk.ac.diamond.daq.memory.persistence.service.PersistenceException;
//import uk.ac.diamond.daq.ogm.persistence.domain.PersistableItem;
//import uk.ac.diamond.daq.ogm.persistence.domain.Plan;
//import uk.ac.diamond.daq.ogm.persistence.domain.Scan;
//import uk.ac.diamond.daq.ogm.persistence.domain.ScanMap;
//
//public class NativeNeoPersistenceService {
//
//    private PersistableItemService itemServ = new PersistableItemService();
//    private ScanService scanServ = new ScanService();
//    private RotationalStepScanService rotatScanServ = new RotationalStepScanService();
//    private SimpleStepScanService stepScanServ = new SimpleStepScanService();
//    private PlanService planServ = new PlanService();
//    private ScanMapService scanMapServ = new ScanMapService();
//
//    private Service[] iterateService = new Service[]{rotatScanServ, stepScanServ, scanServ, planServ, scanMapServ,
//            itemServ};
//
//    public void createOrUpdate(PersistableItem item) {
//        if (item instanceof Scan) {
//            scanServ.createOrUpdate((Scan) item);
//            return;
//        }
//        if (item instanceof Plan) {
//            planServ.createOrUpdate((Plan) item);
//            return;
//        }
//        if (item instanceof ScanMap) {
//            scanMapServ.createOrUpdate((ScanMap) item);
//            return;
//        }
//        itemServ.createOrUpdate((PersistableItem) item);
//
//    }
//
//    public Iterable<PersistableItem> findAll(Class type) throws PersistenceException {
//        for (Service serv : iterateService) {
//            if (serv.getClass().equals(type)) {
//                return serv.findAll();
//            }
//        }
//        throw new PersistenceException("Type not found");
//    }
//
//    public void delete(Long id) {
//        itemServ.delete(id);
//    }
//
//    public PersistableItem find(Long id) {
//        return itemServ.find(id);
//    }
//
//}
