package uk.ac.diamond.daq.module;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import uk.ac.diamond.daq.persistence.repository.*;

class Module extends AbstractModule {

	@Override
	protected void configure() {
		bind(SimpleStepScanService.class).to(SimpleStepScanServiceImpl.class).in(Scopes.SINGLETON);
		bind(RotationalStepScanService.class).to(RotationalStepScanServiceImpl.class).in(Scopes.SINGLETON);
		bind(ScanService.class).to(ScanServiceImpl.class).in(Scopes.SINGLETON);
		bind(ScanMapService.class).to(ScanMapServiceImpl.class).in(Scopes.SINGLETON);
		bind(PlanService.class).to(PlanServiceImpl.class).in(Scopes.SINGLETON);
	}
}