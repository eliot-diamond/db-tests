package uk.ac.diamond.daq.peristence.data;

import uk.ac.diamond.daq.peristence.annotation.Listable;

public class DiffractionScan extends Scan {
    private int x;
    private int y;
    private int width;
    private int height;

    public DiffractionScan(long id, String name, int x, int y, int width, int height) {
        super(id, name);

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
}
