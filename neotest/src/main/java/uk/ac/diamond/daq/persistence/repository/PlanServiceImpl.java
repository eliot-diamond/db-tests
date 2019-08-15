package uk.ac.diamond.daq.persistence.repository;

import uk.ac.diamond.daq.persistence.domain.Plan;

public class PlanServiceImpl extends GenericService<Plan> implements PlanService {

	@Override
	Class<Plan> getEntityType() {
		return Plan.class;
	}

}
