package org.aksw.sparql_integrate.cli.main;

/**
 * Idea to rewrite analytic queries to operations on RDDs.
 * If a query only has a BGP that has a star join on the subject then it can
 * be executed based on a scan of the subject-graphs of a subject-sorted rdd.
 * Certain aggregation over the whole dataset can be rewritten as a
 * local aggregation on the subject graph and a global one that accumulates the
 * local contributions.
 *
 */
//public class SansaQueryRewrite
//    extends TransformBase
//{
//
//
//
//    public static pushGroup(OpGroup op, OpService target) {
//        for (ExprAggregator ea : op.getAggregators())
//        }
//
//        // op.getGroupVars()
//
//    }
//
//    public static Expr pushAggCount(AggCount agg, Query inner) {
//        agg.getExpr();
//
//        Expr innerCount = inner.allocAggregate(AggregatorFactory.createCount(false));
//    }
//
//    public static Expr pushAggSum(AggAvg agg, Query inner) {
//        Expr expr = agg.getExprList().get(0);
//
//        Expr count = inner.allocAggregate(AggregatorFactory.createCount(false));
//        Expr sum = inner.allocAggregate(AggregatorFactory.createSum(false, expr));
//
//        Expr result = new E_Conditional(
//                E_NotEquals(count, NodeValue.makeInteger(0)),
//                new E_Divide(sum, count),
//                NodeValue.makeInteger(0));
//        return result;
//    }
//}
