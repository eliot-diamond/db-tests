package uk.ac.diamond.daq.persistence.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Persisted;

public class LoadTrigger extends Trigger {
    private static final Logger log = LoggerFactory.getLogger(LoadTrigger.class);
    @Persisted
    private double maximumLoad;

    @JsonCreator
    public LoadTrigger(@JsonProperty("name") String name, @JsonProperty("scan") Scan scan,
                       @JsonProperty("maximumLoad") double maximumLoad) {
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
