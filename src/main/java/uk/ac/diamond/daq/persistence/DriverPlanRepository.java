package uk.ac.diamond.daq.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverPlanRepository extends CrudRepository<String, Object> {
}