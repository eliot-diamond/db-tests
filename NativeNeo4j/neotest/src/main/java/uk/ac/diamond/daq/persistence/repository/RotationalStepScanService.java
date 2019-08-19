package uk.ac.diamond.daq.persistence.repository;

import uk.ac.diamond.daq.persistence.domain.RotationalStepScan;

public class RotationalStepScanService extends GenericService<RotationalStepScan> {

    @Override
    Class getEntityType() {
        return RotationalStepScan.class;
    }
}
