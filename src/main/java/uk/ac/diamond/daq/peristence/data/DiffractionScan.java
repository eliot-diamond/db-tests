package uk.ac.diamond.daq.peristence.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.peristence.annotation.Listable;
import uk.ac.diamond.daq.peristence.annotation.Persisted;

import java.io.Serializable;

public class DiffractionScan extends Scan implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(DiffractionScan.class);

    @Persisted
    private int x;
    @Persisted
    private int y;
    @Persisted
    private int width;
    @Persisted
    private int height;

    public DiffractionScan(String name, int x, int y, int width, int height) {
        super(name);

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Listable(value = "X", priority = 1)
    public int getX() {
        return x;
    }

    @Listable(value = "Y", priority = 2)
    public int getY() {
        return y;
    }

    @Listable(value = "Width", priority = 3)
    public int getWidth() {
        return width;
    }

    @Listable(value = "Height", priority = 4)
    public int getHeight() {
        return height;
    }

    @Override
    public void run() {
        log.info("Running scan {} at x: {}, y: {} with width: {}, height: {}", getName(), x, y, width, height);
    }
}
