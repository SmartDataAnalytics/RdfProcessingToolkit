package org.aksw.sparql_binding_stream.cli.main;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.aksw.commons.io.util.StdIo;
import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.rx.RDFLanguagesEx;
import org.aksw.jena_sparql_api.rx.io.resultset.NamedGraphStreamCliUtils;
import org.aksw.jenax.arq.util.binding.ResultSetUtils;
import org.aksw.jenax.sparql.query.rx.RDFDataMgrRx;
import org.aksw.jenax.sparql.query.rx.ResultSetRx;
import org.aksw.jenax.sparql.query.rx.ResultSetRxImpl;
import org.aksw.jenax.sparql.rx.op.ResultSetRxOps;
import org.aksw.jenax.stmt.parser.query.SparqlQueryParser;
import org.aksw.jenax.stmt.parser.query.SparqlQueryParserImpl;
import org.aksw.jenax.stmt.parser.query.SparqlQueryParserWrapperSelectShortForm;
import org.aksw.sparql_binding_stream.cli.cmd.CmdSbsFilter;
import org.aksw.sparql_binding_stream.cli.cmd.CmdSbsMap;
import org.apache.hadoop.shaded.org.apache.commons.compress.utils.CloseShieldFilterInputStream;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.ExprUtils;
import org.apache.jena.sparql.util.NodeFactoryExtra;

import io.reactivex.rxjava3.core.Flowable;

public class SbsCmdImpls {


    /**
     *
     * Reads the result set header from a stream and returns it.
     * The provided input stream must support marks
     *
     * @param tin
     * @return
     * @throws IOException
     */
    public static List<Var> readResultSetHeader(InputStream in, Lang lang) throws IOException {
        if (!in.markSupported()) {
            throw new IllegalArgumentException("Supplied input stream must have support for marks");
        }

        in.mark(100 * 1024 * 1024);

        List<Var> result;
        try (InputStream shielded = new CloseShieldFilterInputStream(in)) {
            ResultSet rs = ResultSetMgr.read(shielded, lang);
            try {
                result = ResultSetUtils.getVars(rs);
            } finally {
                rs.close();
            }
        }

        in.reset();
        return result;
    }

    public static List<Var> readResultSetHeader(TypedInputStream in) throws IOException {
        String ct = in.getContentType();
        Lang lang = RDFLanguages.contentTypeToLang(ct);
        List<Var> result = readResultSetHeader(in, lang);
        return result;
    }


//    public static ResultSet createResultSet(TypedInputStream tin) {
//        String ct = tin.getContentType();
//        Lang lang = RDFLanguages.contentTypeToLang(ct);
//        ResultSet result = ResultSetMgr.read(tin, lang);
//        return result;
//    }

    public static ResultSetRx createResultSetRx(Callable<TypedInputStream> inSupp) {
        ResultSetRx result;
        try(TypedInputStream tin = inSupp.call()) {
            List<Var> vars = readResultSetHeader(tin);
            Flowable<Binding> flowable = RDFDataMgrRx.createFlowableBindings(inSupp);
            result = ResultSetRxImpl.create(vars, flowable);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }


    public static ResultSetRx createResultSetRx(String filenameOrIri, Collection<Lang> probeLangs)  {
        Callable<TypedInputStream> inSupp = NamedGraphStreamCliUtils.validate(filenameOrIri, probeLangs, true);

        ResultSetRx result = createResultSetRx(inSupp);
        return result;
    }

    /**
     * Create a union ResultSetRx from multiple ones.
     * The header variables become the union of those of the members
     *
     * @param members
     * @return
     */
    public static ResultSetRx union(Collection<ResultSetRx> members) {
        ResultSetRx result;

        if (members.isEmpty()) {
            throw new IllegalArgumentException("At least one union member required");
        } else if (members.size() == 1) {
            result = members.iterator().next();
        } else {
            Set<Var> newVars = new LinkedHashSet<>();

            List<Flowable<Binding>> flowables = new ArrayList<>();
            for(ResultSetRx member : members) {
                List<Var> contribVars = member.getVars();
                newVars.addAll(contribVars);

                Flowable<Binding> contribFlow = member.getBindings();
                flowables.add(contribFlow);
            }

            Flowable<Binding> newBindigs = Flowable.concat(flowables);
            result = ResultSetRxImpl.create(new ArrayList<>(newVars), newBindigs);
        }

        return result;
    }

    public static ResultSetRx createResultSetRxFromArgs(List<String> rawArgs) {
        List<String> args = NamedGraphStreamCliUtils.preprocessArgs(rawArgs);

        List<Lang> resultSetProbeLangs = RDFLanguagesEx.getResultSetProbeLangs();

        List<ResultSetRx> rss = args.stream()
            .map(arg -> SbsCmdImpls.createResultSetRx(arg, resultSetProbeLangs))
            .collect(Collectors.toList());

        ResultSetRx result = union(rss);

        return result;
    }


    public static FunctionEnv createExecCxt() {
        Context context = ARQ.getContext().copy() ;
        context.set(ARQConstants.sysCurrentTime, NodeFactoryExtra.nowAsDateTime()) ;
        FunctionEnv env = new ExecutionContext(context, null, null, null) ;

        return env;
    }

    public static int filter(CmdSbsFilter cmd) throws Exception {
        List<Lang> resultSetFormats = RDFLanguagesEx.getResultSetFormats();
        Lang outLang = RDFLanguagesEx.findLang(cmd.outFormat, resultSetFormats);
        ResultSetRx in = createResultSetRxFromArgs(cmd.nonOptionArgs);

        PrefixMapping pm = DefaultPrefixes.get();

        ExprList exprs = new ExprList();
        for (String arg : cmd.exprs) {
            Expr e = ExprUtils.parse(arg, pm);
            exprs.add(e);
        }

        Function<ResultSetRx, ResultSetRx> xform = ResultSetRxOps.createTransformFilter(exprs, createExecCxt());

        ResultSetRx out = xform.apply(in);

        try(QueryExecution e = out.asQueryExecution()) {
            ResultSet rs = e.execSelect();

            ResultSetMgr.write(StdIo.openStdOutWithCloseShield(), rs, outLang);
        }

        return 0;
    }

    public static int query(CmdSbsMap cmd) throws Exception {

        List<Lang> resultSetFormats = RDFLanguagesEx.getResultSetFormats();
        Lang outLang = RDFLanguagesEx.findLang(cmd.outFormat, resultSetFormats);

        Prologue p = new Prologue(DefaultPrefixes.get());
        SparqlQueryParser queryParser = SparqlQueryParserImpl.wrapWithOptimizePrefixes(SparqlQueryParserWrapperSelectShortForm.wrap(
                SparqlQueryParserImpl.create(Syntax.syntaxARQ, p)));

        Query query = queryParser.apply(cmd.queries.get(0));

        ResultSetRx in = createResultSetRxFromArgs(cmd.nonOptionArgs);

        Function<ResultSetRx, ResultSetRx> xform = ResultSetRxOps.createTransformForGroupBy(query, createExecCxt());

        ResultSetRx out = xform.apply(in);

        try(QueryExecution e = out.asQueryExecution()) {
            ResultSet rs = e.execSelect();

            ResultSetMgr.write(StdIo.openStdOutWithCloseShield(), rs, outLang);
        }

        return 0;
    }
}
