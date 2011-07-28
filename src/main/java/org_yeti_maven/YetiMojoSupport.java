/*
 * Copyright 2011 Christian Essl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org_yeti_maven;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org_yeti_maven_executions.JavaMainCaller;
import org_yeti_maven_executions.JavaMainCallerInProcess;
import org_yeti_maven_executions.MainHelper;

public abstract class YetiMojoSupport extends AbstractMojo {

    public static final String YETI_GROUPID= "org.yeti";
    public static final String YETI__ARTIFACTID= "yeti";
    public static final String YETI_LIB_ARTIFACTID= "yeti-lib";
    public static final String YETI_MAVEN_ARTIFACTID="yeti-maven-plugin";
    public static final String YETICL_ARTIFACTID="yeticl";

    public static final String YETI_VERSION = "0.1-SNAPSHOT";
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     * @required
     * @readonly
     */
    protected ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     * @required
     * @readonly
     */
    protected ArtifactResolver resolver;
    /**
     * Location of the local repository.
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected ArtifactRepository localRepo;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected List<?> remoteRepos;

    /**
     * Additional dependencies/jar to add to classpath to run "yetiClassName" (scope and optional field not supported)
     * ex :
     * <pre>
     *    &lt;dependencies>
     *      &lt;dependency>
     *        &lt;groupId>org.yeti&lt;/groupId>
     *        &lt;artifactId>yeti&lt;/artifactId>
     *        &lt;version>0.1-SNAPSHOT&lt;/version>
     *      &lt;/dependency>
     *    &lt;/dependencies>
     * </pre>
     * @parameter
     */
    protected BasicArtifact[] dependencies;

    /**
     * Jvm Arguments.
     *
     * @parameter
     */
    protected String[] jvmArgs;

    /**
     * compiler additionnals arguments
     *
     * @parameter
     */
    protected String[] args;

    /**
     * version of the yeti tool to provide as
     *
     * @required
     * @parameter expression="${yeti.maven.version}"
     *            default-value="0.1-SNAPSHOT"
     */
     protected String yetiVersion;

    /**
     * wheter to include fullyeti jar
     *
     * @required
     * @parameter expression="${yeti.full-jar}"
     *            default-value="true"
     */
     protected boolean yetiFullJar = true;

    /**
     * wheter to include yeti lib jar
     *
     * @required
     * @parameter expression="${yeti.lib-jar}"
     *            default-value="true"
     */
     protected boolean yetiLibJar = true;

    /**
     * className (FQN) of the yeti tool to provide as
     *
     * @required
     * @parameter expression="${yeti.maven.className}"
     *            default-value="yeti.lang.compiler.yeti"
     */
    protected String yetiClassName;

    /**
     * Display the command line called ?
     * 
     *
     * @required
     * @parameter expression="${displayCmd}"
     *            default-value="false"
     */
    public boolean displayCmd;


    /**
     * Force the use of an external ArgFile to run any forked process.
     *
     * @parameter default-value="false"
     */
    protected boolean forceUseArgFile = false;

    /**
     * Artifact factory, needed to download source jars.
     *
     * @component
     * @required
     * @readonly
     */
    protected MavenProjectBuilder mavenProjectBuilder;

    /**
     * The artifact repository to use.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * The artifact factory to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * The artifact collector to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactCollector artifactCollector;

    /**
     * The dependency tree builder to use.
     *
     * @component
     * @required
     * @readonly
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

    protected void prepareIncludes(Set<String> includes, boolean sendJavaToYetic) {
        if (includes.isEmpty()) {
            includes.add("**/*.yeti");
            includes.add("**/*.teti");

            if (sendJavaToYetic) {
                includes.add("**/*.java");
            }
        }
    }

    /**
     * This method resolves the dependency artifacts from the project.
     *
     * @param theProject The POM.
     * @return resolved set of dependency artifacts.
     *
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     * @throws InvalidDependencyVersionException
     */
    @SuppressWarnings("unchecked")
    protected Set<Artifact> resolveDependencyArtifacts(MavenProject theProject) throws Exception {
        AndArtifactFilter filter = new AndArtifactFilter();
        filter.add(new ScopeArtifactFilter(Artifact.SCOPE_TEST));
        filter.add(new ArtifactFilter(){
            public boolean include(Artifact artifact) {
                return !artifact.isOptional();
            }
        });
        //TODO follow the dependenciesManagement and override rules
        Set<Artifact> artifacts = theProject.createArtifacts(factory, Artifact.SCOPE_RUNTIME, filter);
        for (Artifact artifact : artifacts) {
            resolver.resolve(artifact, remoteRepos, localRepo);
        }
        return artifacts;
    }

    /**
     * This method resolves all transitive dependencies of an artifact.
     *
     * @param artifact the artifact used to retrieve dependencies
     *
     * @return resolved set of dependencies
     *
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     * @throws ProjectBuildingException
     * @throws InvalidDependencyVersionException
     */
    protected Set<Artifact> resolveArtifactDependencies(Artifact artifact) throws Exception {
        Artifact pomArtifact = factory.createArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), "", "pom");
        MavenProject pomProject = mavenProjectBuilder.buildFromRepository(pomArtifact, remoteRepos, localRepo);
        return resolveDependencyArtifacts(pomProject);
    }

    public void addToClasspath(String groupId, String artifactId, String version, Set<String> classpath) throws Exception {
        addToClasspath(groupId, artifactId, version, classpath, true);
    }


    public void addToClasspath(String groupId, String artifactId, String version, Set<String> classpath, boolean addDependencies) throws Exception {
        addToClasspath(factory.createArtifact(groupId, artifactId, version, Artifact.SCOPE_RUNTIME, "jar"), classpath, addDependencies);
    }

    protected void addToClasspath(Artifact artifact, Set<String> classpath, boolean addDependencies) throws Exception {
        resolver.resolve(artifact, remoteRepos, localRepo);
        classpath.add(artifact.getFile().getCanonicalPath());
        if (addDependencies) {
            for (Artifact dep : resolveArtifactDependencies(artifact)) {
                //classpath.add(dep.getFile().getCanonicalPath());
                addToClasspath(dep, classpath, addDependencies);
            }
        }
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            doExecute();
        } catch (MojoExecutionException exc) {
            throw exc;
        } catch (MojoFailureException exc) {
            throw exc;
        } catch (RuntimeException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MojoExecutionException("wrap: " + exc, exc);
        }
    }

    @SuppressWarnings("unchecked")
    protected List<Dependency> getDependencies() {
        return project.getCompileDependencies();
    }





    protected abstract void doExecute() throws Exception;

    protected void addYetiLibToClassPath(Set<String> classpath) throws Exception {
        if(yetiFullJar){
            addToClasspath(YETI_GROUPID, YETI_ARTIFACTID, yetiVersion, classpath);
        }else{
            if(yetiLibJar)
                addToClasspath(YETI_GROUPID, YETI_LIB_ARTIFACTID, yetiVersion, classpath);
        }

    }



}
