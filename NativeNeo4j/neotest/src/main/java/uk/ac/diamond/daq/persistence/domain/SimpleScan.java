package uk.ac.diamond.daq.persistence.domain;

import org.neo4j.ogm.annotation.Property;

public abstract class SimpleScan extends Scan {

    @Property
    private double x;
    @Property
    private double y;
    @Property
    private double z;

    protected SimpleScan(SimpleScan source) {
        super(source);
    }

    public SimpleScan() {
        super();
    }

}
