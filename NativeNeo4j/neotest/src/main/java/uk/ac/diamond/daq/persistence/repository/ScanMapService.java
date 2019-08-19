package uk.ac.diamond.daq.persistence.repository;

import uk.ac.diamond.daq.persistence.domain.ScanMap;

public class ScanMapService extends GenericService<ScanMap> {

    @Override
    Class getEntityType() {
        return ScanMap.class;
    }
}
