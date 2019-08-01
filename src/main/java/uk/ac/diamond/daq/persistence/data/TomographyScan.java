package uk.ac.diamond.daq.persistence.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.annotation.Searchable;

import java.util.Objects;

public class TomographyScan extends Scan {
    private static final Logger log = LoggerFactory.getLogger(TomographyScan.class);

    @Persisted
    @Listable("Frames")
    @Searchable("frames")
    private int frames;

    @Persisted
    @Listable("Rotation Angle")
    @Searchable("angle")
    private double angle;

    public TomographyScan(String name, int frames, double angle) {
        super(name);

        this.frames = frames;
        this.angle = angle;
    }

    public double getAngle() {
        return angle;
    }

    public int getFrames() {
        return frames;
    }

    public void setFrames(int frames) {
        this.frames = frames;
    }

    @Override
    public void run() {
        log.info("Running scan {} (id: {}, version: {}) with {} frames over an angle of {}", getName(), getId(), getVersion(), frames, angle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TomographyScan that = (TomographyScan) o;
        return frames == that.frames &&
                Double.compare(that.angle, angle) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), frames, angle);
    }
}
