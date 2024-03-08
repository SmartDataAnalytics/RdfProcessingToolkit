package org.aksw.bench.geo.cmd;

import java.io.PrintWriter;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.aksw.commons.io.util.StdIo;
import org.aksw.commons.picocli.CmdCommonBase;
import org.apache.jena.geosparql.implementation.GeometryWrapper;
import org.apache.jena.geosparql.implementation.jts.CustomGeometryFactory;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.riot.out.NodeFmtLib;
import org.locationtech.jts.geom.Envelope;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "query")
public class CmdBenchGenGridQueryGen
    extends CmdCommonBase
    implements Callable<Integer>
{
    @ArgGroup(exclusive = false)
    protected GridOffsets gridOffsets = new GridOffsets();

    static class GridOffsets {
        @Option(names = "--minX", required = true, defaultValue = "-180")
        protected double minX;

        @Option(names = "--maxX", required = true, defaultValue = "180")
        protected double maxX;

        @Option(names = "--minY", required = true, defaultValue = "-90")
        protected double minY;

        @Option(names = "--maxY", required = true, defaultValue = "90")
        protected double maxY;
    }

    @Option(names = "--rows", required = true, defaultValue = "2")
    protected int rows;

    @Option(names = "--cols", required = true, defaultValue = "2")
    protected int cols;

    @Option(names = "--allGraphs", required = true, defaultValue = "false")
    protected boolean allGraphs;


    public static Query genQuery(Cell2D cell, String fixedGraph) {

        String g = fixedGraph == null ? "?g" : NodeFmtLib.strNT(NodeFactory.createURI(fixedGraph));

        String queryStr = """
            PREFIX geo: <http://www.opengis.net/ont/geosparql#>
            PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
            PREFIX spatial: <http://jena.apache.org/spatial#>
            SELECT (COUNT(*) AS ?c) {
              GRAPH $GRAPH$ {
                BIND($CELLGEOM$ AS ?queryGeom)
                ?feature spatial:intersectBoxGeom (?queryGeom) .
                ?feature geo:hasGeometry ?featureGeom .
                ?featureGeom geo:asWKT ?featureGeomWkt .
                FILTER(geof:sfIntersects(?featureGeomWkt, ?queryGeom))
              }
            }
            """
                .replace("$CELLGEOM$", NodeFmtLib.strNT(toNode(cell.envelope())))
                .replace("$GRAPH$", g)
                ;

        System.out.println(queryStr);
        Query result = QueryFactory.create(queryStr);
        return result;
    }

    public static Node toNode(Envelope envelope) {
        return new GeometryWrapper(CustomGeometryFactory.theInstance().toGeometry(envelope), Geo.WKT).asNode();
    }

    @Override
    public Integer call() throws Exception {
        Grid2D grid = Grid2D.newBuilder()
            .setMinX(gridOffsets.minX)
            .setMaxX(gridOffsets.maxX)
            .setMinX(gridOffsets.minY)
            .setMaxX(gridOffsets.maxY)
            .setRowCount(rows)
            .setColCount(cols)
            .build();

        String fixedGraph = allGraphs ? null : CmdBenchGenGridDataGen.genGraphName(0);

        Stream<Query> queries = grid.stream().map(cell -> genQuery(cell, fixedGraph));

        try (PrintWriter writer = new PrintWriter(StdIo.openStdOutWithCloseShield())) {
            queries.forEach(query -> writer.println(query));
            writer.flush();
        }
        return 0;
    }
}
