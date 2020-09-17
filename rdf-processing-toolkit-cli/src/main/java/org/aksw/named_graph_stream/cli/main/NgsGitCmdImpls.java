package org.aksw.named_graph_stream.cli.main;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map.Entry;

import org.aksw.jena_sparql_api.rx.DatasetFactoryEx;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsGit;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class NgsGitCmdImpls {
    private static final Logger logger = LoggerFactory.getLogger(NgsGitCmdImpls.class);

    public static int git(CmdNgsGit cmd) throws Exception {
        OutputStream out = MainCliNamedGraphStream.out;

        List<String> filenames = cmd.nonOptionArgs;

        Multimap<Path, Path> repoToFiles = ArrayListMultimap.create();

        boolean isError = false;
        for (String filename : filenames) {
            File file = new File(filename).getAbsoluteFile();
            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            repositoryBuilder.findGitDir(file.getParentFile());

            File gitDir = repositoryBuilder.getGitDir();
            if (gitDir == null) {
                logger.error("No git repo detected for file: " + file + "");
                isError = true;
            } else {
                Path repoPath = gitDir.toPath().toAbsolutePath().normalize().getParent();
                Path filePath = file.toPath().toAbsolutePath().normalize();

                Path relFilePath = repoPath.relativize(filePath);
                repoToFiles.put(repoPath, relFilePath);
            }
        }

        int result;
        if(isError) {
            result = 1;
        } else {
            process(repoToFiles, out);

            result = 0;
        }

        return result;
    }


    public static void process(Multimap<Path, Path> repoToFiles, OutputStream out) throws Exception {

        for (Entry<Path, Collection<Path>> e : repoToFiles.asMap().entrySet()) {
            Path repoPath = e.getKey();
            Collection<Path> files = e.getValue();

            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            Repository repo = repositoryBuilder
                .findGitDir(repoPath.toFile())
                .setMustExist(true)
                .build();

            try(Git git = Git.wrap(repo)) {

                for (Path file : files) {
                    process(git, file.toString(), out);
                }
            }
        }



//        Git gitRepo = Git.open(fullPath.toFile());
    }


    // TODO Make this return a Flowable Dataset
    public static void process(Git gitRepo, String filename, OutputStream out)
            throws Exception {

        Iterable<RevCommit> revCommits = gitRepo.log()
                .addPath(filename)
                .call();

        for (RevCommit revCommit : revCommits) {
            try(TreeWalk treeWalk = TreeWalk.forPath(gitRepo.getRepository(), filename, revCommit.getTree())) {

                try (InputStream in = gitRepo.getRepository().open(treeWalk.getObjectId(0), Constants.OBJ_BLOB).openStream()) {
                    Lang lang = RDFDataMgr.determineLang(filename, null, null);
                    Model model = DatasetFactoryEx.createInsertOrderPreservingDataset().getDefaultModel();
                    RDFDataMgr.read(model, in, lang);
                    //TypedInputStream tin = RDFDataMgrEx.probeLang(in, Arrays.asList(RDFLanguages.TTL));
                    // RDFDataMgr.read(dataset, in, la);
                    //RDFDataMgrEx.read(model, tin);


                    PersonIdent authorIdent = revCommit.getAuthorIdent();
                    Date date = authorIdent.getWhen();
                    Instant instant = date.toInstant();
                    ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
                    Calendar cal = GregorianCalendar.from(zdt);
                    XSDDateTime dt = new XSDDateTime(cal);

                    //revCommit.toObjectId()

                    String graphName = "urn:git:" + revCommit.getName() + "-" + instant;

                    //System.err.println(graphName);

                    Dataset dataset = DatasetFactoryEx.createInsertOrderPreservingDataset();

                    Model tgt = dataset.getNamedModel(graphName);

                    Resource metadata = tgt.createResource(graphName);
                    metadata
                        .addLiteral(ResourceFactory.createProperty("urn:git:timestamp"), dt)
                        .addLiteral(ResourceFactory.createProperty("urn:git:name"), revCommit.getName());

                    tgt.add(model);
                    RDFDataMgr.write(out, dataset, RDFFormat.TRIG_BLOCKS);
    //                System.out.println("Triples: " + model.size());
                }
            }
        }
    }
}
