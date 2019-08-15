package uk.ac.diamond.daq.persistence.repository;

import uk.ac.diamond.daq.persistence.domain.Scan;

public class ScanServiceImpl extends GenericService<Scan> implements ScanService {

	@Override
	Class getEntityType() {
		return Scan.class;
	}

}
