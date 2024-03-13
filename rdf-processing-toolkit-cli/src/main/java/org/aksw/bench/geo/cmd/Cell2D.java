package org.aksw.bench.geo.cmd;

import org.locationtech.jts.geom.Envelope;

public record Cell2D(Grid2D grid, int row, int col, Envelope envelope) {
    public int id() {
        return row * grid.sizeY + col;
    }
}
