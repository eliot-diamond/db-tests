package uk.ac.diamond.daq.persistence.repository;

import uk.ac.diamond.daq.persistence.domain.Scan;

public class ScanService extends GenericService<Scan> {

    @Override
    Class getEntityType() {
        return Scan.class;
    }
}
