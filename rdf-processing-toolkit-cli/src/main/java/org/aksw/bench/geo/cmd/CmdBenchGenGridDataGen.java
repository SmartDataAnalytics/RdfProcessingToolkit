package org.aksw.bench.geo.cmd;

import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;
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
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.sparql.core.Quad;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.util.AffineTransformation;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "data")
// versionProvider = VersionProviderRdfProcessingToolkit.class,
// description = "Run sequences of SPARQL queries and stream triples, quads and bindings")
public class CmdBenchGenGridDataGen
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

    @Option(names = "--graphs", required = true, defaultValue = "2")
    protected int graphs;

    @Option(names = "--scale", required = true, defaultValue = "false", fallbackValue = "true", description = "Make polygons smaller the higher the graph id.")
    protected boolean scale;


    public static Query genQuery(Cell2D cell) {
//        ?adm spatial:intersectBoxGeom(?river_geom) .
//        filter(geof:sfIntersects(?river_geom, ?adm_geom_))

        String queryStr = """
            PREFIX geo: <http://www.opengis.net/ont/geosparql#>
            PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
            PREFIX spatial-f: <http://jena.apache.org/function/spatial#>
            SELECT (COUNT(*) AS ?c) {
              GRAPH ?g {
                BIND($CELLGEOM$ AS ?queryGeom)
                ?feature spatial-f:intersectsBoxGeom ?queryGeom .
                ?feature geo:hasGeometry ?featureGeom .
                ?feature geo:asWKTLiteral ?featureGeomWkt .
                FILTER(geof:sfIntersects(?featureGeomWkt, ?queryGeom))
              }
            }
            """.replace("$CELLGEOM$", NodeFmtLib.strNT(toNode(toGeometry(cell.envelope()))))
        ;

        System.out.println(queryStr);
        Query result = QueryFactory.create(queryStr);
        return result;

//    	Node intersectsBoxGeom = NodeFactory.createURI(SpatialExtension.INTERSECT_BOX_GEOM_PROP);
//    	Node sfIntersects = NodeFactory.createURI(Geof.SF_INTERSECTS);

//    	Var feature = Var.alloc("feature");
//    	Var geometry = Var.alloc("geometry");
//
//        Query query = new Query();
//        query.setQuerySelectType();
//        query.setQueryPattern(ElementUtils.unionIfNeeded(
//        	new ElementNamedGraph(Vars.g,
//	            ElementUtils.createElementTriple(
//	                Triple.create(
//	            )
//	        )
//        );
//
//

    }

    public static Geometry toGeometry(Envelope envelope) {
        return CustomGeometryFactory.theInstance().toGeometry(envelope);
    }

    public static Node toNode(Geometry geom) {
        return new GeometryWrapper(geom, Geo.WKT).asNode();
    }

//    public static Stream<Geometry> envelopeToGeometry(Stream<Envelope> in) {
//        return in.map(env -> CustomGeometryFactory.theInstance().toGeometry(env));
//    }
//
//    public static Stream<Node> jtsToJena(Stream<Geometry> in) {
//        return in.map(geom -> new GeometryWrapper(geom, Geo.WKT).asNode());
//    }

    public static String genGraphName(int id) {
        return "https://www.example.org/graph/" + id;
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

        Stream<Quad> quads =
            IntStream.range(0, graphs).boxed().flatMap(g -> {
                double ratio = scale ? 1 - g / (double)graphs : 1.0;
                Node graph = NodeFactory.createURI(genGraphName(g));
                return grid.stream().flatMap(cell -> {
                    Node feature = NodeFactory.createURI("https://www.example.org/feature/" + g + "/" + cell.row() + "/" + cell.col());
                    Node geom = NodeFactory.createURI("https://www.example.org/geometry/" + g + "/" + cell.row() + "/" + cell.col());

                    Geometry cellGeom = toGeometry(cell.envelope());
                    Geometry scaledCellGeom = ratio != 1.0
                            ? scaleGeometry(cellGeom, ratio, ratio)
                            : cellGeom;
                    Node wkt = toNode(scaledCellGeom);

                    // System.out.println(genQuery(cell));

                    return Stream.of(
                        Quad.create(graph, feature, Geo.HAS_GEOMETRY_NODE, geom),
                        Quad.create(graph, geom, Geo.AS_WKT_NODE, wkt)
                    );
                });
            });

        try (OutputStream out = StdIo.openStdOutWithCloseShield()) {
            StreamRDF writer = StreamRDFWriter.getWriterStream(out, RDFFormat.NQUADS);
            writer.start();
            quads.forEach(writer::quad);
            writer.finish();
            out.flush();
        }
        return 0;
    }


    public static Geometry scaleGeometry(Geometry geometry, double scaleFactorX, double scaleFactorY) {
        Point centroid = geometry.getCentroid();
        AffineTransformation scaleTrans = AffineTransformation.scaleInstance(scaleFactorX, scaleFactorY, centroid.getX(), centroid.getY());
        return scaleTrans.transform(geometry);
    }
}
