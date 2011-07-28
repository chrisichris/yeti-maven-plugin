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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;

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
     * Enables/Disables sending java source to the yeti compiler.
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
    private List<String> _sourceFiles;
    private boolean _filterPrinted = false;

    @SuppressWarnings("unchecked")
    protected List<File> getSourceDirectories() throws Exception {
        List<String> sources = project.getCompileSourceRoots();
        //Quick fix in case the user has not added the "add-source" goal.
        String yetiSourceDir = sourceDir.getCanonicalPath();
        if (!sources.contains(yetiSourceDir)) {
            sources.add(yetiSourceDir);
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
    private List<String> findSourceFiles() {
        if (_sourceFiles == null) {
            try {
                List<String> sourceFiles = new ArrayList<String>();
                List<File> sourceRootDirs = getSourceDirectories();
                initFilters();

                for (File dir : sourceRootDirs) {
                    String[] tmpFiles = MainHelper.findFiles(dir, includes.toArray(new String[includes.size()]), excludes.toArray(new String[excludes.size()]));
                    for (String tmpLocalFile : tmpFiles) {
                        if (new File(dir,tmpLocalFile).exists()) {
                            String localName = tmpLocalFile.replace(File.separator, "/");
                            sourceFiles.add(localName);
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

    public boolean canGenerateReport() {
        return true;
    }

    public boolean isExternalReport() {
        return true;
    }

    public String getCategoryName() {
        return CATEGORY_PROJECT_REPORTS;
    }

    public String getDescription(@SuppressWarnings("unused") Locale locale) {
        if (StringUtils.isEmpty(description)) {
            return "Yeti API documentation";
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
    public void generate(@SuppressWarnings("unused") Sink sink, @SuppressWarnings("unused") Locale locale) throws MavenReportException {
        try {
            if (!canGenerateReport()) {
                getLog().warn("No source files found");
                return;
            }


            long t0 = System.currentTimeMillis();

            //The sourceDirs
            List<File> sourceDirs = getSourceDirectories();
            String[] sourceDirsA = new String[sourceDirs.size()];
            for(int i= 0;i < sourceDirsA.length;i++) {
                sourceDirsA[i] = sourceDirs.get(i).getPath();
            }

            //prepare the compile/doc classloader
            Object compileInstance = null;
            Method compileMethod = null;
            ClassLoader compileClassLoader = null;
            {
                Set<String> classpath = new HashSet<String>();
                addToClasspath(YETI_GROUPID, YETI_ARTIFACTID, yetiVersion, classpath);
                addToClasspath(YETI_GROUPID, YETICL_ARTIFACTID, yetiVersion, classpath);
                classpath.addAll(project.getCompileClasspathElements());
                String[] classPathUrls = classpath.toArray(new String[classpath.size()]);

                if(classPathUrls != null && classPathUrls.length > 0) {
                    List urls = new ArrayList();
                    for(int i=0;i < classPathUrls.length; i++) {
                        try {
                            urls.add(new File(classPathUrls[i]).toURI().toURL());
                            if(displayCmd) {
                                getLog().info(new File(classPathUrls[i]).toURI().toURL().toString());
                            }
                        } catch (MalformedURLException ex) {
                            throw new IllegalArgumentException("Could not make URL of file:"+classPathUrls[i]+" reason: "+ex.getMessage(),ex);
                        }
                    }
                    URL[] urlsA = (URL[]) urls.toArray(new URL[urls.size()]);
                    compileClassLoader = new URLClassLoader(urlsA,ClassLoader.getSystemClassLoader());

                    //Class sourceReaderClass = compileClassLoader.loadClass("yeti.lang.compiler.SourceReader");
                    Class compileClass = compileClassLoader.loadClass("org.yeticl.YetiCompileHelper");
                    compileMethod = compileClass.getMethod("htmlDocAll", String[].class, String[].class,Boolean.TYPE,String[].class,String.class);
                    compileInstance = compileClass.newInstance();
                    //sourceReader = compileClassLoader.loadClass("org.yeticl.FileSourceReader").getConstructor(String[].class).newInstance((Object)sds);


                }else{
                    getLog().error("No classpath this must not happen");
                    throw new IllegalStateException("No classpath here");
                }
            }

            List<String> sourceFilesC = findSourceFiles();
            String[] sourceFiles = sourceFilesC.toArray(new String[sourceFilesC.size()]);


            File reportOutputDir = getReportOutputDirectory();
            if (!reportOutputDir.exists()) {
                reportOutputDir.mkdirs();
            }

            if (getLog().isDebugEnabled()) {
                for(File directory : sourceDirs) {
                    getLog().debug(directory.getCanonicalPath());
                }
            }

            String[] classPath = new String[]{};

            long t1 = System.currentTimeMillis();
            getLog().info(String.format("Compiling %d source files to %s at %d", sourceFilesC.size(), reportOutputDir.getAbsolutePath(), t1));

            ClassLoader oldL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(compileClassLoader);
            try{

                String toPath = reportOutputDir.getAbsolutePath();
                toPath = (toPath.equals("") || toPath.endsWith("/")) ? toPath : toPath + "/";
                compileMethod.invoke(compileInstance, classPath,sourceDirsA,false,sourceFiles,toPath);

            }catch(InvocationTargetException ex) {
                if(ex.getCause() instanceof Exception) {
                    Exception e = (Exception) ex.getCause();
                    if("yeti.lang.compiler.CompileException".equals(e.getClass().getName()))
                        throw new MojoExecutionException(e.getMessage());
                    else
                        throw e;
                }else throw ex;
            }finally{
                Thread.currentThread().setContextClassLoader(oldL);
            }

            getLog().info(String.format("prepare-compile in %d s", (t1 - t0) / 1000));
            getLog().info(String.format("compile in %d s", (System.currentTimeMillis() - t1) / 1000));


 
        } catch (RuntimeException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MavenReportException("wrap: " + exc.getMessage(), exc);
        }
    }

}
