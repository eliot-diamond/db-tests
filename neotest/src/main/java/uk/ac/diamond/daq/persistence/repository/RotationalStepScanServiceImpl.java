package uk.ac.diamond.daq.persistence.repository;

import uk.ac.diamond.daq.persistence.domain.RotationalStepScan;

public class RotationalStepScanServiceImpl extends GenericService<RotationalStepScan>
		implements RotationalStepScanService {

	@Override
	Class getEntityType() {
		return RotationalStepScan.class;

	}

}
