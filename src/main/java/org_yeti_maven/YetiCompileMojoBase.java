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

    
    protected File normalize(File f) {
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
                if (!newCompileSourceRootsList.contains(srcDirFile) 
						&& srcDirFile.exists()) {
                    newCompileSourceRootsList.add(srcDirFile);
                }
            }
        }
        return newCompileSourceRootsList;
    }


    @SuppressWarnings("unchecked")
    protected List<String> getClasspathElements() throws Exception {
        List<String> fs = 
			TychoUtilities.addOsgiClasspathElements(project, 
						project.getCompileClasspathElements());
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
        List<File> r = new ArrayList<File>();
		if(sourceDir.exists())
			r.add(normalize(sourceDir));
		return r;
    }

    /**
     * @return
     * @throws Exception
     */
    private List<String> findSourceFiles(List<File> sourceRootDirs) {
        try {
            List<String> sourceFiles = new ArrayList<String>();

            //make sure filter is ok
            prepareIncludes(includes);
            for (File dir : sourceRootDirs) {
                String[] tmpFiles = 
					MainHelper.findFiles(dir, 
							includes.toArray(new String[includes.size()]), 
							excludes.toArray(new String[excludes.size()]));
                for (String tmpLocalFile : tmpFiles) {
                    File fl = new File(dir,tmpLocalFile);
					if (fl.exists())
						sourceFiles.add(fl.getPath());
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
        long t0 = System.currentTimeMillis();

        //the classpath
		Set<String> classpath = 
			new HashSet<String>(getClasspathElements());

		//the sourcedirs and files
        List<File> sourceDirs = getSourceDirectories();
        
        List<String> sourceFiles = findSourceFiles(sourceDirs);

        //output dir
		File outputDir = normalize(getOutputDir());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
		String toPath = outputDir.getAbsolutePath();
		toPath = (toPath.equals("") || toPath.endsWith("/")) ? 
				toPath : toPath + "/";
        
		//some logging
		if (getLog().isDebugEnabled()) {
            for(File directory : sourceDirs) {
                getLog().debug(directory.getCanonicalPath());
            }
        }

        getLog().info(String.format("Compiling %d source files to %s at %s %s ", 
					sourceFiles.size(), outputDir.getAbsolutePath(),
					sourceFiles,
					sourceDirs));

	
		List<String> params = new ArrayList<String>();
		params.add("-d");
		params.add(toPath);
		params.addAll(sourceFiles);
		for(File f:sourceDirs)params.add(f.getPath());

		invokeYeti(classpath, params.toArray(new String[params.size()]));

        getLog().info(String.format("compile in %d s", 
					(System.currentTimeMillis() - t0) / 1000));
    }
}
