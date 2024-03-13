package org.aksw.bench.geo.cmd;

import picocli.CommandLine.Option;

public class GridOffsets {
    @Option(names = "--minX", defaultValue = "-90") // -180
    protected double minX;

    @Option(names = "--maxX", defaultValue = "90") // 180
    protected double maxX;

    @Option(names = "--minY", defaultValue = "-90")
    protected double minY;

    @Option(names = "--maxY", defaultValue = "90")
    protected double maxY;

    public double getMinX() {
        return minX;
    }

    public void setMinX(double minX) {
        this.minX = minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public void setMaxX(double maxX) {
        this.maxX = maxX;
    }

    public double getMinY() {
        return minY;
    }

    public void setMinY(double minY) {
        this.minY = minY;
    }

    public double getMaxY() {
        return maxY;
    }

    public void setMaxY(double maxY) {
        this.maxY = maxY;
    }
}
