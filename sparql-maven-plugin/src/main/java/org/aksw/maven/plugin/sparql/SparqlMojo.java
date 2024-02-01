/*
 * Copyright 2013 Luca Tagliani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aksw.maven.plugin.sparql;

import java.util.List;
import java.util.Map;

import org.aksw.commons.util.derby.DerbyUtils;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain.OutputSpec;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Goal which generate a version list.
 *
 */
@Mojo(name = "run") // TODO Consider rename to "integrate" in order to align with rdf-processing-toolkit
public class SparqlMojo extends AbstractMojo {

    static { DerbyUtils.disableDerbyLog(); }

    /** The repository system (Aether) which does most of the management. */
    @Component
    private RepositorySystem repoSystem;

    /** The current repository/network configuration of Maven. */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /** The project's remote repositories to use for the resolution of project dependencies. */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> projectRepos;

    /**
     * Starting version
     */
//    @Parameter(defaultValue = "${startingVersion}", required = true)
//    private String startingVersion;
    /**
     * Starting version
     */
//    @Parameter(defaultValue = "false", required = true)
//    private boolean includeSnapshots;
    /**
     * GroupId of project.
     */
//    @Parameter(defaultValue = "${project.groupId}", required = true)
//    private String groupId;
    /**
     * ArtifactId of project.
     */
//    @Parameter(defaultValue = "${project.artifactId}", required = true)
//    private String artifactId;
    /**
     * Name of the property that contains the ordered list of versions requested.
     */
//    @Parameter(defaultValue = "${project.artifactId}", required = true)
//    private String versionListPropertyName;
    /**
     * The Maven project
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * The SPARQL engine to use for processing
     */
    @Parameter(defaultValue = "mem")
    private String engine;

    /**
     * The SPARQL engine to use for processing
     */
    @Parameter
    private String tmpdir;

    /**
     * Properties for use in substitution
     */
    @Parameter
    private Map<String, String> env;

    /**
     * Arguments of the SPARQL processor
     */
    @Parameter
    private List<String> args;

    /**
     * Output file
     */
    @Parameter
    private String outputFile;

    /**
     * Output format
     */
    @Parameter
    private String outputFormat;


    @Override
    public void execute() throws MojoExecutionException {
        try {
            CmdSparqlIntegrateMain cmd = new CmdSparqlIntegrateMain();
            cmd.nonOptionArgs = args;
            if (outputFile != null) {
                cmd.outputSpec = new OutputSpec();
                cmd.outputSpec.outFile = outputFile;
            }
            cmd.engine = engine;
            cmd.outFormat = outputFormat;
            cmd.env = env;
            cmd.debugMode = true;

            cmd.call();
//
//            // create the artifact to search for
//            Artifact artifact = new DefaultArtifact(groupId, artifactId, project.getPackaging(), "[" + startingVersion + ",)");
//            // create the version request object
//            VersionRangeRequest rangeRequest = new VersionRangeRequest();
//            rangeRequest.setArtifact(artifact);
//            rangeRequest.setRepositories(projectRepos);
//            // search for the versions
//            VersionRangeResult rangeResult = repoSystem.resolveVersionRange(repoSession, rangeRequest);
//            getLog().info("Retrieving version of " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getExtension());
//            List<Version> availableVersions = rangeResult.getVersions();
//            getLog().info("Available versions " + availableVersions);
//            // if we don't want the snapshots, filter them
//            if (!includeSnapshots) {
//                filterSnapshots(availableVersions);
//            }
//            // order the version from the newer to the older
//            Collections.reverse(availableVersions);
//            ArrayList<String> versionList = new ArrayList<>();
//            availableVersions.forEach((version) -> {
//                versionList.add(version.toString());
//            });
//            // set the poject property
//            project.getProperties().put(versionListPropertyName, versionList);
        } catch (Exception ex) {
            throw new MojoExecutionException("Error in plugin", ex); //ex.getCause());
        }
    }

//    private void filterSnapshots(List<Version> versions) {
//        for (Iterator<Version> versionIterator = versions.iterator(); versionIterator.hasNext();) {
//            Version version = versionIterator.next();
//            // if the version is a snapshot, get rid of it
//            if (version.toString().endsWith("SNAPSHOT")) {
//                versionIterator.remove();
//            }
//        }
//    }
}
