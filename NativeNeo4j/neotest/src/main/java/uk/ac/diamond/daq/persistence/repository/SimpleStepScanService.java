package uk.ac.diamond.daq.persistence.repository;

import uk.ac.diamond.daq.persistence.domain.SimpleStepScan;

public class SimpleStepScanService extends GenericService<SimpleStepScan> {
    @Override
    Class getEntityType() {
        return SimpleStepScan.class;
    }
}
