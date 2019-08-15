package uk.ac.diamond.daq;

import org.neo4j.ogm.*;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.SessionFactory;

import uk.ac.diamond.daq.persistence.domain.*;
import uk.ac.diamond.daq.persistence.repository.*;

public class Runner {
	
	private static ScanService scanRepo = new ScanServiceImpl();

	public static void main(String... args) throws Exception {
		Scan firstScan = new RotationalStepScan();
		firstScan.setCaptureDevice("BIG_ONE");
		Scan secondScan = new SimpleStepScan();
		secondScan.setName("2nd Scan");

		Trigger firstTrigger = new Trigger();
		Trigger secondTrigger = new Trigger();
		Trigger thirdTrigger = new Trigger();

		firstTrigger.setScan(firstScan);
		secondTrigger.setScan(secondScan);
		firstTrigger.setCondition("When the moon is full");

		Plan firstPlan = new Plan();
		firstPlan.addTrigger(firstTrigger);
		firstPlan.addTrigger(secondTrigger);
		firstPlan.addTrigger(thirdTrigger);

		scanRepo.createOrUpdate(firstPlan);
		
	}
}