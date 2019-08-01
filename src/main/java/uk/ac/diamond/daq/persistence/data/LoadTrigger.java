package uk.ac.diamond.daq.persistence.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Persisted;

import java.io.Serializable;

public class LoadTrigger extends Trigger implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(LoadTrigger.class);
    @Persisted
    private double maximumLoad;

    public LoadTrigger(String name, Scan scan, double maximumLoad) {
        super(name, scan);
        this.maximumLoad = maximumLoad;
    }

    @Override
    public void validate() {
        log.info("Trigger {} (id: {}, version: {}) due to maximum load {}", getName(), getId(), getVersion(), maximumLoad);
        getScan().run();
    }

    public double getMaximumLoad() {
        return maximumLoad;
    }
}
