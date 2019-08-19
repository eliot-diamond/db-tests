package uk.ac.diamond.daq.persistence.domain;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

@NodeEntity
public class SimpleStepScan extends SimpleScan {

    @Property
    private Direction direction;

    @Property
    private double initial;
    @Property
    private double maximum;
    @Property
    private double stepSize;

    protected SimpleStepScan(SimpleStepScan source) {
        super(source);
        this.initial = source.initial;
        this.maximum = source.maximum;
        this.stepSize = source.stepSize;
    }

    public SimpleStepScan() {
        super();
    }

    @Override
    protected void increment() {
        this.previousVersion = new SimpleStepScan(this);
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public double getInitial() {
        return initial;
    }

    public void setInitial(double initial) {
        this.initial = initial;
    }

    public double getMaximum() {
        return maximum;
    }

    public void setMaximum(double maximum) {
        this.maximum = maximum;
    }

    public double getStepSize() {
        return stepSize;
    }

    public void setStepSize(double stepSize) {
        this.stepSize = stepSize;
    }

    private enum Direction {
        X, Y, Z
    }

}
