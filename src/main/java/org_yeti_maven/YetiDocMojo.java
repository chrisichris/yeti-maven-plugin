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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;
import org_yeti_maven_executions.JavaMainCaller;
import org_yeti_maven_executions.MainHelper;

/**
 * Produces Yeti API documentation.
 *
 * @goal doc
 * @requiresDependencyResolution compile
 * @execute phase="generate-sources"
 */
public class YetiDocMojo extends YetiMojoSupport implements MavenReport {

    /**
     * Specify title of generated HTML documentation.
     *
     * @parameter expression="${title}"
     *            default-value="${project.name} ${project.version} API"
     */
    protected String title;
    /**
     * Specifies the destination directory where yetiDoc saves the generated
     * HTML files.
     *
     * @parameter expression="yetidocs"
     * @required
     */
    private String outputDirectory;
    /**
     * Specifies the destination directory where yetidoc saves the generated HTML files.
     *
     * @parameter expression="${project.reporting.outputDirectory}/yetidocs"
     * @required
     */
    private File reportOutputDirectory;
    /**
     * The name of the YetiDoc report.
     *
     * @since 2.1
     * @parameter expression="${name}" default-value="YetiDocs"
     */
    private String name;
    /**
     * The description of the Yetidoc report.
     *
     * @since 2.1
     * @parameter expression="${description}" default-value="YetiDoc API
     *            documentation."
     */
    private String description;
    /**
     * className (FQN) of the main yetidoc to use, if not define, the the ClassName is used
     *
     * @parameter expression="${yeti.maven.doc.className}"
     */
    protected String yetidocClassName;
    /**
     * To allow running aggregation only from command line use "-Dforce-aggregate=true" (avoid using in pom.xml).
     * [scaladoc, vscaladoc]
     *
     * @parameter expression="${force-aggregate}" default-value="false"
     */
    protected boolean forceAggregate = false;
    /**
     * If you want to aggregate only direct sub modules.
     *
     * @parameter expression="${yeti.maven.doc.aggregateDirectOnly}" default-value="true"
     */
    protected boolean aggregateDirectOnly = true;
    /**
     * The directory which contains yeti source files
     *
     * @parameter expression="${project.build.sourceDirectory}/../yeti"
     */
    protected File sourceDir;
    /**
     * Enables/Disables sending java source to the scala compiler.
     *
     * @parameter default-value="false"
     */
    protected boolean sendJavaToYetic = false;
    /**
     * A list of inclusion filters for the compiler.
     * ex :
     * <pre>
     *    &lt;includes&gt;
     *      &lt;include&gt;SomeFile.yeti&lt;/include&gt;
     *    &lt;/includes&gt;
     * </pre>
     *
     * @parameter
     */
    protected Set<String> includes = new HashSet<String>();
    /**
     * A list of exclusion filters for the compiler.
     * ex :
     * <pre>
     *    &lt;excludes&gt;
     *      &lt;exclude&gt;SomeBadFile.yeti&lt;/exclude&gt;
     *    &lt;/excludes&gt;
     * </pre>
     *
     * @parameter
     */
    protected Set<String> excludes = new HashSet<String>();
    private List<File> _sourceFiles;
    private boolean _filterPrinted = false;

    @SuppressWarnings("unchecked")
    protected List<File> getSourceDirectories() throws Exception {
        List<String> sources = project.getCompileSourceRoots();
        //Quick fix in case the user has not added the "add-source" goal.
        String scalaSourceDir = sourceDir.getCanonicalPath();
        if (!sources.contains(scalaSourceDir)) {
            sources.add(scalaSourceDir);
        }
        List<File> ret = new ArrayList<File>(sources.size());
        for (String path : sources) {
            File f = new File(path);
            try {
                f = f.getCanonicalFile();
            } catch (IOException exc) {
                f = f.getAbsoluteFile();
            }
            if (f.exists() && !ret.contains(f)) {
                ret.add(f);
            }
        }
        return ret;
    }

    protected void initFilters() throws Exception {
        prepareIncludes(includes, sendJavaToYetic);
    }

    /**
     * @return
     * @throws Exception
     */
    private List<File> findSourceFiles() {
        if (_sourceFiles == null) {
            try {
                List<File> sourceFiles = new ArrayList<File>();
                List<File> sourceRootDirs = getSourceDirectories();
                initFilters();

                for (File dir : sourceRootDirs) {
                    String[] tmpFiles = MainHelper.findFiles(dir, includes.toArray(new String[includes.size()]), excludes.toArray(new String[excludes.size()]));
                    for (String tmpLocalFile : tmpFiles) {
                        if (new File(dir,tmpLocalFile).exists()) {
                            sourceFiles.add(new File(tmpLocalFile));
                        }
                    }
                }
                Collections.sort(sourceFiles);

                _sourceFiles = sourceFiles;
            } catch (Exception exc) {
                throw new RuntimeException("can't define source to process", exc);
            }
        }
        return _sourceFiles;
    }

    private boolean canAggregate() {
        return false;
    }

    public boolean canGenerateReport() {
        // there is modules to aggregate
        boolean back = ((project.isExecutionRoot() || forceAggregate) && canAggregate() && project.getCollectedProjects().size() > 0);
        back = back || (findSourceFiles().size() != 0);
        return back;
    }

    public boolean isExternalReport() {
        return true;
    }

    public String getCategoryName() {
        return CATEGORY_PROJECT_REPORTS;
    }

    public String getDescription(@SuppressWarnings("unused") Locale locale) {
        if (StringUtils.isEmpty(description)) {
            return "YetiDoc API documentation";
        }
        return description;
    }

    public String getName(@SuppressWarnings("unused") Locale locale) {
        if (StringUtils.isEmpty(name)) {
            return "YetiDocs";
        }
        return name;
    }

    public String getOutputName() {
        return outputDirectory + "/index";
    }

    public File getReportOutputDirectory() {
        if (reportOutputDirectory == null) {
            reportOutputDirectory = new File(project.getBasedir(), project.getReporting().getOutputDirectory() + "/" + outputDirectory).getAbsoluteFile();
        }
        return reportOutputDirectory;
    }

    public void setReportOutputDirectory(File v) {
        if (v != null && outputDirectory != null && !v.getAbsolutePath().endsWith(outputDirectory)) {
            this.reportOutputDirectory = new File(v, outputDirectory);
        } else {
            this.reportOutputDirectory = v;
        }
    }

    @Override
    public void doExecute() throws Exception {
        // SiteRendererSink sink = siteRenderer.createSink(new
        // File(project.getReporting().getOutputDirectory(), getOutputName() +
        // ".html");
        generate(null, Locale.getDefault());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected JavaMainCaller getScalaCommand() throws Exception {
        //This ensures we have a valid scala version...
        if (StringUtils.isEmpty(yetidocClassName)) {
            yetidocClassName = "org.yetidoc.yetidoc";
        }

        JavaMainCaller jcmd = getEmptyScalaCommand(yetidocClassName);
        jcmd.addArgs(args);
        jcmd.addJvmArgs(jvmArgs);

        List<String> paths = project.getCompileClasspathElements();
        //paths.remove(project.getBuild().getOutputDirectory()); //remove output to avoid "error for" : error:  XXX is already defined as package XXX ... object XXX {
        jcmd.addOption("-cp", MainHelper.toMultiPath(paths));
        //jcmd.addOption("-sourcepath", sourceDir.getAbsolutePath());
        if (StringUtils.isEmpty(title)) {
            jcmd.addOption("-fm", title);
        }
        return jcmd;
    }

    public void generate(@SuppressWarnings("unused") Sink sink, @SuppressWarnings("unused") Locale locale) throws MavenReportException {
        try {
            if (!canGenerateReport()) {
                getLog().warn("No source files found");
                return;
            }

            File reportOutputDir = getReportOutputDirectory();
            if (!reportOutputDir.exists()) {
                reportOutputDir.mkdirs();
            }
            {
                BasicArtifact artifact = new BasicArtifact();
                artifact.artifactId = "yetidoc";
                artifact.groupId = "org.yeti";
                artifact.version = yetiVersion;
                dependencies = new BasicArtifact[]{artifact};
            }

            List<File> sources = findSourceFiles();
            if (sources.size() > 0) {
                JavaMainCaller jcmd = getScalaCommand();

                jcmd.addOption("-d", reportOutputDir.getAbsolutePath());

                List<File> sourceDirs = getSourceDirectories();
                List<String> sourceDirPathes = new ArrayList<String>();
                for (File x : sourceDirs) {
                    sourceDirPathes.add(x.getAbsolutePath());
                }
                jcmd.addOption("-sd", MainHelper.toMultiPath(sourceDirPathes));

                for (File x : sources) {
                    jcmd.addArgs(x.getPath());
                }
                jcmd.run(displayCmd);
            }

            /*
            if (forceAggregate) {
                aggregate(project);
            } else {
                // Mojo could not be run from parent after all its children
                // So the aggregation will be run after the last child
                tryAggregateUpper(project);
            }
            */
        } catch (MavenReportException exc) {
            throw exc;
        } catch (RuntimeException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MavenReportException("wrap: " + exc.getMessage(), exc);
        }
    }

    @SuppressWarnings("unchecked")
    protected void tryAggregateUpper(MavenProject prj) throws Exception {
        if (prj != null && prj.hasParent() && canAggregate()) {
            MavenProject parent = prj.getParent();
            List<MavenProject> modules = parent.getCollectedProjects();
            if ((modules.size() > 1) && prj.equals(modules.get(modules.size() - 1))) {
                aggregate(parent);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void aggregate(MavenProject parent) throws Exception {
        if(! canAggregate())
            return;
        List<MavenProject> modules = parent.getCollectedProjects();
        File dest = new File(parent.getReporting().getOutputDirectory() + "/" + outputDirectory);
        getLog().info("start aggregation into " + dest);
        StringBuilder mpath = new StringBuilder();
        for (MavenProject module : modules) {
            if ("pom".equals(module.getPackaging().toLowerCase())) {
                continue;
            }
            if (aggregateDirectOnly && module.getParent() != parent) {
                continue;
            }
            File subScaladocPath = new File(module.getReporting().getOutputDirectory() + "/" + outputDirectory).getAbsoluteFile();
            if (subScaladocPath.exists()) {
                mpath.append(subScaladocPath).append(File.pathSeparatorChar);
            }
        }
        if (mpath.length() != 0) {
            getLog().info("aggregate vscaladoc from : " + mpath);
            JavaMainCaller jcmd = getScalaCommand();
            jcmd.addOption("-d", dest.getAbsolutePath());
            jcmd.addOption("-aggregate", mpath.toString());
            jcmd.run(displayCmd);
        } else {
            getLog().warn("no vscaladoc to aggregate");
        }
        tryAggregateUpper(parent);
    }
}
