package org.aksw.data_profiler.cli;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.aksw.commons.graph.index.core.SubgraphIsomorphismIndex;
import org.aksw.commons.graph.index.core.SubgraphIsomorphismIndexWrapper;
import org.aksw.commons.graph.index.jena.SubgraphIsomorphismIndexJena;
import org.aksw.commons.graph.index.jena.transform.QueryToGraph;
import org.aksw.commons.jena.jgrapht.PseudoGraphJenaGraph;
import org.aksw.jena_sparql_api.algebra.utils.AlgebraUtils;
import org.aksw.jena_sparql_api.algebra.utils.OpExtConjunctiveQuery;
import org.aksw.jena_sparql_api.algebra.utils.OpUtils;
import org.aksw.jena_sparql_api.stmt.SparqlStmtMgr;
import org.aksw.jena_sparql_api.stmt.SparqlStmtParser;
import org.aksw.jena_sparql_api.stmt.SparqlStmtParserImpl;
import org.aksw.jena_sparql_api.utils.QueryUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.optimize.Rewrite;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.vocabulary.VOID;
import org.jgrapht.graph.DefaultGraphType;

import com.google.common.graph.Traverser;

public class MainCliSportalAnalysis {

    public static Map<Path, Query> loadQueries(Path startPath) throws IOException {
        List<String> exclusions = Arrays.asList(/* no exclusions as all test cases work */);

        PathMatcher pathMatcher = startPath.getFileSystem().getPathMatcher("glob:**/*.sparql");
        PrefixMapping pm = new PrefixMappingImpl();
        pm.setNsPrefix("v", VOID.NS);
        pm.setNsPrefix("e", "http://sportal.ex/base#");
        pm.setNsPrefix("s", "http://sportal.ex/s#");
        SparqlStmtParser parser = SparqlStmtParserImpl.create(pm);

        //List<Query> result = new ArrayList<Query>();
        Map<Path, Query> result = new LinkedHashMap<>();
        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(pathMatcher.matches(file)) {
                    boolean isExcluded = exclusions.stream()
                            .anyMatch(suffix -> file.toString().endsWith(suffix));

                    if(!isExcluded) {
                        String queryStr = Files.lines(file).collect(Collectors.joining("\n"));
//                        try {
                            //Query query = QueryFactory.create(queryStr);
//                        System.out.println("processing file " + file);
                        Query query = parser.apply(queryStr).getQuery();
                            result.put(file, query);
//                        } catch(Exception e) {
//                            // Silently ignore queries that fail to parse
//                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return result;
    }


    public static void main(String[] args) throws Exception {
        JenaSystem.init();
//        Query q = SparqlStmtMgr.loadQuery("compact/qf6.sparql");
//        Rewrite rewrite = AlgebraUtils.createDefaultRewriter();
//        q = QueryUtils.rewrite(q, rewrite::rewrite);
//        System.out.println(q);


        SubgraphIsomorphismIndex<String, Graph, Node> base =
                SubgraphIsomorphismIndexWrapper.wrap(
                        SubgraphIsomorphismIndexJena.create(),
                        jenaGraph -> new PseudoGraphJenaGraph(jenaGraph, DefaultGraphType.directedSimple()));

        SubgraphIsomorphismIndex<String, Query, Node> index =
                SubgraphIsomorphismIndexWrapper.wrap(base, QueryToGraph::queryToGraph);


        Path startPath = Paths.get("./src/main/resources").toAbsolutePath().normalize();

        Map<Path, Query> queries = loadQueries(startPath);

        List<String> excludes = Arrays.asList("qf6", "qf7", "qf8");
        for(Entry<Path, Query> e : queries.entrySet()) {

            String key = e.getKey().getFileName().toString();

            boolean skip = excludes.stream().anyMatch(x -> key.contains(x));
            if(skip) {
                System.out.println("Skipped: " + key);
                continue;
            }

            // Exclude union queries qf6 qf7 qf8 - the union could be decomposed into
            // a set of disjunctive queries that are added separately to the index
            // they could be added as qfxy - where
            // x is the query ID and y is the union member



            Query query = e.getValue();
            Op op = Algebra.compile(query);
            Op cqOp = QueryToGraph.normalizeOp(op, false);
            Op leaf = Traverser.forTree(OpUtils::getSubOps).depthFirstPostOrder(cqOp).iterator().next();

            OpExtConjunctiveQuery x = (OpExtConjunctiveQuery)leaf;
            Op standardOp = x.effectiveOp();
            Query qq = OpAsQuery.asQuery(standardOp);

            //AlgebraUtils.tryExtractConjunctiveQuery(cqOp, generator);

            //AlgebraUtils.tryExtractConjunctiveQuery(op, generator);
            // System.out.println(qq);
            index.put(key, qq);
        }

        index.printTree();
        //System.out.println(index);


    }
}
