package uk.ac.diamond.daq.persistence.repository;

import uk.ac.diamond.daq.persistence.domain.Plan;

public class PlanService extends GenericService<Plan> {


    @Override
    Class getEntityType() {
        return Plan.class;
    }
}
}
