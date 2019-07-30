package uk.ac.diamond.daq.peristence.data;

import uk.ac.diamond.daq.peristence.annotation.Listable;
import uk.ac.diamond.daq.peristence.annotation.Searchable;

public class TomographyScan extends Scan {
    @Listable("Frames")
    @Searchable("frames")
    private int frames;

    @Listable("Rotation Angle")
    @Searchable("angle")
    private double angle;

    public TomographyScan(long id, String name, int frames, double angle) {
        super(id, name);

        this.frames = frames;
        this.angle = angle;
    }

    public double getAngle() {
        return angle;
    }

    public int getFrames() {
        return frames;
    }
}
