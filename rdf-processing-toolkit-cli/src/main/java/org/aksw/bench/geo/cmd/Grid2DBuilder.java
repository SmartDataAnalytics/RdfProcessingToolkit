package org.aksw.bench.geo.cmd;

public class Grid2DBuilder {

    protected double minX;
    protected double maxX;

    protected double minY;
    protected double maxY;

    protected int colCount;
    protected int rowCount;

    // TODO Make GeometryFactory configurable

    protected Grid2DBuilder() {
        super();
        this.minX = -180;
        this.maxX = 180;
        this.minY = -90;
        this.maxY = 90;
        this.colCount = 2;
        this.rowCount = 2;
    }

    public double getMinX() {
        return minX;
    }

    public Grid2DBuilder setMinX(double minX) {
        this.minX = minX;
        return this;
    }

    public double getMaxX() {
        return maxX;
    }

    public Grid2DBuilder setMaxX(double maxX) {
        this.maxX = maxX;
        return this;
    }

    public double getMinY() {
        return minY;
    }

    public Grid2DBuilder setMinY(double minY) {
        this.minY = minY;
        return this;
    }

    public double getMaxY() {
        return maxY;
    }

    public Grid2DBuilder setMaxY(double maxY) {
        this.maxY = maxY;
        return this;
    }

    public int getSizeX() {
        return colCount;
    }

    public Grid2DBuilder setColCount(int colCount) {
        this.colCount = colCount;
        return this;
    }

    public int getRowCount() {
        return rowCount;
    }

    public Grid2DBuilder setRowCount(int rowCount) {
        this.rowCount = rowCount;
        return this;
    }

    public Grid2D build() {
        return new Grid2D(minX, maxX, minY, maxY, colCount, rowCount);
    }

    @Override
    public String toString() {
        return "GridBuilder [minX=" + minX + ", maxX=" + maxX + ", minY=" + minY
                + ", maxY=" + maxY + ", sizeX=" + colCount + ", sizeY=" + rowCount + "]";
    }
}
