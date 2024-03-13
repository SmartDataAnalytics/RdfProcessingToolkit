package org.aksw.bench.geo.cmd;

import java.io.PrintWriter;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.aksw.commons.io.util.StdIo;
import org.aksw.commons.picocli.CmdCommonBase;
import org.aksw.jenax.arq.util.syntax.QueryUtils;
import org.aksw.jenax.arq.util.var.Vars;
import org.apache.jena.geosparql.implementation.GeometryWrapper;
import org.apache.jena.geosparql.implementation.jts.CustomGeometryFactory;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Transform;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.OpGraph;
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

    @Option(names = "--rows", required = true, defaultValue = "2")
    protected int rows;

    @Option(names = "--cols", required = true, defaultValue = "2")
    protected int cols;

    @Option(names = "--graphs", required = true, defaultValue = "2")
    protected int graphs;

//    @ArgGroup(exclusive = true, multiplicity = "0..1")
//    public GraphSpec outputSpec;
//
//    public static class GraphSpec {
//        /**
//         * sparql-pattern file
//         *
//         */
//        @Option(names = { "-o", "--out-file" }, description = "output file")
//        public String outFile;
//
//        @Option(names = { "--io", },  description = "overwrites argument file on success with output; use with care")
//        public String inOutFile = null;
//    }

    @Option(names = "--allGraphs", required = true, defaultValue = "false")
    protected boolean allGraphs;

    @Option(names = { "-u", "--union-default-graph" }, defaultValue = "false", fallbackValue = "true", description = "If --allGraphs is given. Use union default graph rather than ?g.")
    protected boolean unionDefaultGraphMode;

    @Option(names = "--reverse", required = true, defaultValue = "false", fallbackValue = "true", description = "Generate queries by traversing grid cells from the last to the first.")
    protected boolean reverse;

    public static Query genQuery(Cell2D cell, Node graphNode) {

        String gStr = graphNode == null ? "?dummy" : NodeFmtLib.strNT(graphNode);
        // NodeFmtLib.strNT(NodeFactory.createURI(fixedGraph));

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
                .replace("$GRAPH$", gStr)
                ;

        Query result = QueryFactory.create(queryStr);

        // Drop GRAPH ?foo { } blocks if no graph is given
        if (graphNode == null) {
            Transform xform  = new TransformCopy() {
                @Override
                public Op transform(OpGraph opGraph, Op subOp) {
                    return subOp;
                }
            };

            Op op = Algebra.compile(result);
            op = Transformer.transform(xform, op);
            Query tmp = OpAsQuery.asQuery(op);
            result = QueryUtils.restoreQueryForm(tmp, result);
        }

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
            .setMinY(gridOffsets.minY)
            .setMaxY(gridOffsets.maxY)
            .setRowCount(rows)
            .setColCount(cols)
            .build();

        Stream<Query> queries = grid.stream(!reverse).map(cell -> {
            Node graphNode;
            if (allGraphs) {
                graphNode = unionDefaultGraphMode
                        ? null
                        : Vars.g;
            } else {
                int cellId = cell.id();
                int gid = cellId % graphs;
                graphNode = NodeFactory.createURI(CmdBenchGenGridDataGen.genGraphName(gid));
            }
            Query r = genQuery(cell, graphNode);
            return r;
        });

        try (PrintWriter writer = new PrintWriter(StdIo.openStdOutWithCloseShield())) {
            queries.sequential().forEach(query -> writer.println(query));
            writer.flush();
        }
        return 0;
    }
}
