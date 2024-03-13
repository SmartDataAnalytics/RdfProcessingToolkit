package org.aksw.bench.geo.cmd;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Envelope;

public class Grid2D {
    protected double worldMinX;
    protected double worldMaxX;

    protected double worldMinY;
    protected double worldMaxY;

    protected int sizeX;
    protected int sizeY;

    protected double deltaX;
    protected double deltaY;

    public Grid2D(double worldMinX, double worldMaxX, double worldMinY, double worldMaxY, int sizeX, int sizeY) {
        super();
        this.worldMinX = worldMinX;
        this.worldMaxX = worldMaxX;
        this.worldMinY = worldMinY;
        this.worldMaxY = worldMaxY;
        this.sizeX = sizeX;
        this.sizeY = sizeY;

        this.deltaX = (worldMaxX - worldMinX) / sizeX;
        this.deltaY = (worldMaxY - worldMinY) / sizeY;
    }

    public static IntStream range(int startInclusive, int endExclusive, boolean isForward) {
        return isForward
                ? IntStream.range(startInclusive, endExclusive)
                : IntStream.iterate(endExclusive - 1, i -> i >= startInclusive, i -> i - 1);
    }

    public Stream<Cell2D> stream(boolean isForward) {
        return range(0, sizeY, isForward).mapToObj(row -> row).flatMap(row ->
            range(0, sizeX, isForward).mapToObj(col -> cellOf(row, col)));
    }

    public Cell2D cellOf(int row, int col) {
        double y = worldMinY + (row * deltaY);
        double x = worldMinX + (col * deltaX);
        double dx = x + deltaX;
        double dy = y + deltaY;
        Envelope env = new Envelope(x, dx, y, dy);
        return new Cell2D(this, row, col, env);
    }

    @Override
    public String toString() {
        return "Grid [worldMinX=" + worldMinX + ", worldMaxX=" + worldMaxX + ", worldMinY=" + worldMinY + ", worldMaxY="
                + worldMaxY + ", sizeX=" + sizeX + ", sizeY=" + sizeY + "]";
    }

    public static Grid2DBuilder newBuilder() {
        return new Grid2DBuilder();
    }
}
