package org.aksw.sparql_integrate.cli;

//public class MainCliSparqlStream {
//    private static final Logger logger = LoggerFactory.getLogger(MainCliSparqlStream.class);
//
//
//    public static SPARQLResultSink createSink(
//            String optOutFormat,
//            PrefixMapping pm,
//            PrintStream operationalOut) {
//
//        SPARQLResultSink result;
//
//        Collection<RDFFormat> availableOutRdfFormats = RDFWriterRegistry.registered();
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//
//        Dataset outDataset = DatasetFactoryEx.createInsertOrderPreservingDataset();
//        RDFFormat outFormat = null;
//        if(optOutFormat != null) {
//            if(optOutFormat.equals("jq")) {
//                int depth = 3;
//                boolean jsonFlat = false;
//                result = new SPARQLResultVisitorSelectJsonOutput(null, depth, jsonFlat, gson);
//            } else {
//                outFormat = availableOutRdfFormats.stream()
//                        .filter(f -> f.toString().equalsIgnoreCase(optOutFormat))
//                        .findFirst()
//                        .orElseThrow(() -> new RuntimeException("Unknown format: " + optOutFormat + " Available: " + availableOutRdfFormats));
//
//                Sink<Quad> quadSink = SparqlStmtUtils.createSinkQuads(outFormat, operationalOut, pm, () -> outDataset);
//                result = new SPARQLResultSinkQuads(quadSink);
//            }
//        } else {
//            Sink<Quad> quadSink = SparqlStmtUtils.createSinkQuads(outFormat, operationalOut, pm, () -> outDataset);
//            result = new SPARQLResultSinkQuads(quadSink);
//        }
//
//        return result;
//    }
//
//
//    // Core functionality moved to SparqlScriptProcessor
//    /*
//    public static BiConsumer<RDFConnection, SPARQLResultSink> createProcessor(
//            //CommandMain cliArgs,
//            List<String> args,
//            PrefixMapping pm,
//            boolean closeSink
//            ) throws FileNotFoundException, IOException, ParseException {
//
//        List<BiConsumer<RDFConnection, SPARQLResultSink>> outerParts = new ArrayList<>();
//
//        //List<String> args = cliArgs.nonOptionArgs;
//
////		processor.setShowQuery(args.containsOption("q"));
////		processor.setShowAlgebra(args.containsOption("a"));
//
////		String tmpOutFormat = Optional.ofNullable(args.getOptionValues("w"))
////				.orElse(Collections.emptyList()).stream()
////				.findFirst().orElse(null);
////
////		String optOutFormat = args.containsOption("jq")
////				? "jq"
////				: tmpOutFormat;
////
//
//
//        Path cwd = null;
//        for (String filename : args) {
//            logger.info("Loading argument '" + filename + "'");
//
//            if(filename.startsWith(MainCliSparqlIntegrateOld.cwdKey)) {
//                String cwdValue = filename.substring(MainCliSparqlIntegrateOld.cwdKey.length()).trim();
//
//                if(cwd == null) {
//                    cwd = Paths.get(StandardSystemProperty.USER_DIR.value());
//                }
//
//                cwd = cwd.resolve(cwdValue);
//                logger.info("Pinned working directory to " + cwd);
//            } else if(filename.equals(MainCliSparqlIntegrateOld.cwdResetCwd)) {
//                // If cwdValue is an empty string, reset the working directory
//                logger.info("Unpinned working directory");
//
//                cwd = null;
//            }
//
//            String baseIri = cwd == null ? null : cwd.toUri().toString();
//
//            // Prevent concurrent modifications of prefixes;
//            // processFile will add encountered prefixes
//            PrefixMapping copy = new PrefixMappingImpl();
//            copy.setNsPrefixes(pm);
//
////            SparqlStmtIterator it = SparqlStmtUtils.processFile(copy, filename, baseIri);
//            SparqlStmtParser parser = SparqlStmtParserImpl.create(Syntax.syntaxARQ, new Prologue(copy, baseIri), true);
////            Iterator<SparqlStmt> it = SparqlStmtMgr.loadSparqlStmts(filename, copy, parser, baseIri);
//            Iterator<SparqlStmt> it = SparqlStmtMgr.loadSparqlStmts(filename, parser);
//
//            // View 'it' as a SparqlStmtIterator if applicable; null otherwise
//            SparqlStmtIterator sit = it instanceof SparqlStmtIterator ? (SparqlStmtIterator)it : null;
//
//
//            List<SparqlStmt> stmts = new ArrayList<>();
//
//            while(it.hasNext()) {
//                if(sit != null) {
//                    logger.info("Loading SPARQL statement at line " + sit.getLine() + ", column " + sit.getColumn());
//                }
//                SparqlStmt stmt = it.next();
//                stmt = SparqlStmtUtils.optimizePrefixes(stmt);
//
////				 if(cliArgs.isUnionDefaultGraphMode) {
//                     stmt = SparqlStmtUtils.applyOpTransform(stmt, op -> Transformer
//                             .transformSkipService(new TransformUnionQuery(), op));
////				 }
//                     //Algebra.unionDefaultGraph(op)
//                 stmts.add(stmt);
//            }
//
//            outerParts.add((conn, _sink) -> {
//                SparqlStmtProcessor stmtProcessor = new SparqlStmtProcessor();
//
//                // String inFile = filename;
//                // logger.info("Applying '" + inFile + "'");
//
//                for(SparqlStmt stmt : stmts) {
//                    // Some SPARQL query features are not thread safe - clone them!
//                    SparqlStmt cloneStmt = stmt.clone();
//                    stmtProcessor.processSparqlStmt(conn, cloneStmt, _sink);
//                }
//            });
//
//        }
//
//        BiConsumer<RDFConnection, SPARQLResultSink> result = (conn, sink) -> {
//            //SPARQLResultSink sink = createSink(optOutFormat, pm, operationalOut);
//
//            for(BiConsumer<RDFConnection, SPARQLResultSink> part : outerParts) {
//                part.accept(conn, sink);
//            }
//
//            sink.flush();
//            if(closeSink) {
//                try {
//                    sink.close();
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//        };
//
//        return result;
//    }
//    */
//
//    /**
//     * First non-option argument is interpreted as the input stream
//     *
//     * sparql-stream input.trig *.sparql
//     *
//     * @param args
//     * @throws ParseException
//     * @throws IOException
//     * @throws FileNotFoundException
//     */
//    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {
//        CommandMain cm = new CommandMain();
//
//        JCommander jc = new JCommander.Builder()
//              .addObject(cm)
//              .build();
//
//        jc.parse(args);
//
//        if(cm.nonOptionArgs.isEmpty()) {
//            throw new RuntimeException("Need at least one non-option argument as input");
//        }
//
//        String src = cm.nonOptionArgs.get(0);
//
//        // TODO Reuse code from sparql integrate
//
//        MainCliSparqlIntegrateOld.configureGlobalSettings();
//
//        PrefixMapping pm = new PrefixMappingImpl();
//        pm.setNsPrefixes(DefaultPrefixes.prefixes);
//        JenaExtensionUtil.addPrefixes(pm);
//        JenaExtensionHttp.addPrefixes(pm);
//
//
//        //Function<Dataset, Dataset> processor = null;
//        PrintStream operationalOut = System.out;
//        String optOutFormat = "trig/pretty";
//
//        SPARQLResultSink sink = createSink(optOutFormat, pm, operationalOut);
//        // Skip first argument
//        BiConsumer<RDFConnection, SPARQLResultSink> consumer = createProcessor(
//                cm.nonOptionArgs.subList(1, cm.nonOptionArgs.size()),
//                pm,
//                true);
//
//
//        Flowable<Dataset> datasets = RDFDataMgrRx.createFlowableDatasets(() ->
//            RDFDataMgrEx.prependWithPrefixes(
//                    SparqlStmtUtils.openInputStream(src), pm));
//
//        datasets.forEach(ds -> {
//            Dataset indexedCopy = DatasetFactory.wrap(DatasetGraphFactory.cloneStructure(ds.asDatasetGraph()));
//            try(RDFConnection conn = RDFConnectionFactory.connect(indexedCopy)) {
//                consumer.accept(conn, sink);
//            }
//        });
//    }
//}
