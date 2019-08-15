package uk.ac.diamond.daq.persistence.repository;

import uk.ac.diamond.daq.persistence.domain.SimpleStepScan;

public class SimpleStepScanServiceImpl extends GenericService<SimpleStepScan> implements SimpleStepScanService {

	@Override
	Class getEntityType() {
		return SimpleStepScan.class;
	}

}
