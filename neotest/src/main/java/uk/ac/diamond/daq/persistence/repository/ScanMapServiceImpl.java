package uk.ac.diamond.daq.persistence.repository;

import uk.ac.diamond.daq.persistence.domain.ScanMap;

public class ScanMapServiceImpl extends GenericService<ScanMap> implements ScanMapService {

	@Override
	Class<ScanMap> getEntityType() {
		return ScanMap.class;
	}

}
