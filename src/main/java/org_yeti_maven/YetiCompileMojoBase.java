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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org_yeti_maven_executions.MainHelper;

/**
 * Compiles a directory of Yeti source. Corresponds roughly to the compile goal
 * of the maven-compiler-plugin
 *
 */
public class YetiCompileMojoBase extends YetiMojoSupport {

    /**
     * Enables/Disables sending java source to the yeti compiler.
     *
     * @parameter default-value="false"
     */
    protected boolean sendJavaToYetic = false;


    /**
     * Enables/Disables compilation of yeti source.
     *
     * @parameter expression="${yeti-compile}"
     *            default-value="true"
     */
    protected boolean yetiCompile = true;

    /**
     * Enables/Disables compilation of yeti source.
     *
     * @parameter expression="${yetic}"
     *            default-value="true"
     */
    protected boolean yetiC = true;

    /**
     * A list of inclusion filters for the compiler.
     * ex :
     * <pre>
     *    &lt;includes&gt;
     *      &lt;include&gt;someFile.yeti&lt;/include&gt;
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
     *      &lt;exclude&gt;someBadFile.yeti&lt;/exclude&gt;
     *    &lt;/excludes&gt;
     * </pre>
     *
     * @parameter
     */
    protected Set<String> excludes = new HashSet<String>();
    /**
     * The directory in which to place compilation output
     *
     * @parameter expression="${project.build.outputDirectory}"
     */
    protected File outputDir;

    /**
     * The directory which contains yeti source files
     *
     * @parameter expression="${project.build.sourceDirectory}/../yeti"
     */
    protected File sourceDir;

    /**
     * Wheter to log the compiler warning
     *
     * @parameter default-value="true"
     */
    protected boolean logWarnings = true;

    
    private File normalize(File f) {
        try {
            f = f.getCanonicalFile();
        } catch (IOException exc) {
            f = f.getAbsoluteFile();
        }
        return f;
    }

    /**
     * This limits the source directories to only those that exist for real.
     */
    protected List<File> normalize(List<String> compileSourceRootsList) {
        List<File> newCompileSourceRootsList = new ArrayList<File>();
        if (compileSourceRootsList != null) {
            // copy as I may be modifying it
            for (String srcDir : compileSourceRootsList) {
                File srcDirFile = normalize(new File(srcDir));
                if (!newCompileSourceRootsList.contains(srcDirFile) && srcDirFile.exists()) {
                    newCompileSourceRootsList.add(srcDirFile);
                }
            }
        }
        return newCompileSourceRootsList;
    }


    @SuppressWarnings("unchecked")
    protected List<String> getClasspathElements() throws Exception {
        List<String> fs = TychoUtilities.addOsgiClasspathElements(project, project.getCompileClasspathElements());
        return fs;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Dependency> getDependencies() {
        return project.getCompileDependencies();
    }

    protected File getOutputDir() throws Exception {
        return outputDir.getAbsoluteFile();
    }

    @SuppressWarnings("unchecked")
    protected List<File> getSourceDirectories() throws Exception {
        List<String> sources = project.getCompileSourceRoots();
        //Quick fix in case the user has not added the "add-source" goal.
        String yetiSourceDir = sourceDir.getCanonicalPath();
        if (!sources.contains(yetiSourceDir)) {
            sources.add(yetiSourceDir);
        }
        return normalize(sources);
    }

    /**
     * @return
     * @throws Exception
     */
    private List<String> findSourceFiles(List<File> sourceRootDirs) {
        try {
            List<String> sourceFiles = new ArrayList<String>();

            //make sure filter is ok
            prepareIncludes(includes, sendJavaToYetic);
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
            return sourceFiles;

        } catch (Exception exc) {
            throw new RuntimeException("can't define source to process", exc);
        }
    }
    
  

    @Override
    protected void doExecute() throws Exception {
        if(! (yetiCompile && yetiC ))
                return;
        long t0 = System.currentTimeMillis();
        List<File> sourceDirs = getSourceDirectories();
        String[] sourceDirsA = new String[sourceDirs.size()];
        for(int i= 0;i < sourceDirsA.length;i++) {
            sourceDirsA[i] = sourceDirs.get(i).getPath();
        }

        //prepare the compile classloader
        Object compileInstance = null;
        Method compileMethod = null;
        ClassLoader compileClassLoader = null;
        {
            Set<String> classpath = new HashSet<String>();
            addToClasspath(YETI_GROUPID, YETI_LIBRARY_ARTIFACTID, yetiVersion, classpath);
            addToClasspath(YETI_GROUPID, YETICL_ARTIFACTID, yetiVersion, classpath);
            classpath.addAll(getClasspathElements());
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
                compileMethod = compileClass.getMethod("compileAll", String[].class, String[].class,Boolean.TYPE,String[].class,String.class);
                compileInstance = compileClass.newInstance();
                //sourceReader = compileClassLoader.loadClass("org.yeticl.FileSourceReader").getConstructor(String[].class).newInstance((Object)sds);
               

            }else{
                getLog().error("No classpath this must not happen");
                throw new IllegalStateException("No classpath here");
            }
        }
        
        List<String> sourceFilesC = findSourceFiles(sourceDirs);
        String[] sourceFiles = sourceFilesC.toArray(new String[sourceFilesC.size()]);
        //Collection<String> classPathC =  getClasspathElements();
        String[] classPath = new String[]{};

        File outputDir = normalize(getOutputDir());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        if (getLog().isDebugEnabled()) {
            for(File directory : sourceDirs) {
                getLog().debug(directory.getCanonicalPath());
            }
        }

        long t1 = System.currentTimeMillis();
        getLog().info(String.format("Compiling %d source files to %s at %d", sourceFilesC.size(), outputDir.getAbsolutePath(), t1));

        ClassLoader oldL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(compileClassLoader);
        try{

            String toPath = outputDir.getAbsolutePath();
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

    }


}
