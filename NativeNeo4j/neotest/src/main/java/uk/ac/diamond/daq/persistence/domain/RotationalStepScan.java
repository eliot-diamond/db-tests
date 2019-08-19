package uk.ac.diamond.daq.persistence.domain;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

@NodeEntity
public class RotationalStepScan extends SimpleScan {
    @Property
    private double initialTheta;
    @Property
    private double maxTheta;
    @Property
    private double thetaStep;

    protected RotationalStepScan(RotationalStepScan source) {
        super(source);
        this.initialTheta = source.initialTheta;
        this.maxTheta = source.maxTheta;
        this.thetaStep = source.thetaStep;
    }

    public RotationalStepScan() {
        super();
    }

    @Override
    protected void increment() {
        this.previousVersion = new RotationalStepScan(this);
    }

    public double getInitialTheta() {
        return initialTheta;
    }

    public void setInitialTheta(double initialTheta) {
        increment();
        this.initialTheta = initialTheta;
    }

    public double getMaxTheta() {
        return maxTheta;
    }

    public void setMaxTheta(double maxTheta) {
        increment();
        this.maxTheta = maxTheta;
    }

    public double getThetaStep() {
        return thetaStep;
    }

    public void setThetaStep(double thetaStep) {
        increment();
        this.thetaStep = thetaStep;
    }

}
