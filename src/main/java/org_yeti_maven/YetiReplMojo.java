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
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.codehaus.plexus.util.StringUtils;


/**
 * Run the Yeti Repl with all the classes of the projects (dependencies and builded)
 *
 * @goal repl
 * @requiresDependencyResolution test
 * @execute phase="test-compile"
 * @inheritByDefault false
 * @requiresDirectInvocation true
 * @executionStrategy once-per-session
 * @description runs the yeti console repl
 */
public class YetiReplMojo extends YetiMojoSupport {


    /**
     * Add the test classpath (include classes from test directory), to the repl's classpath ?
     *
     * @parameter expression="${yeti.maven.repl.useTestClasspath}" default-value="true"
     * @required
     */
    protected boolean useTestClasspath;

    /**
     * Add the runtime classpath, to the repl's classpath ?
     *
     * @parameter expression="${yeti.maven.repl.useRuntimeClasspath}" default-value="true"
     * @required
     */
    protected boolean useRuntimeClasspath;

    /**
     * The commands that will be executed line by line on the repl right after it
     * is started and before user input is excepted, lines are seperated by double-semiclon ;;.
     *
     * @parameter expression="${commands}"
     */
    protected String commands;

    /**
     * The directory which contains yeti source files
     *
     * @parameter expression="${project.build.sourceDirectory}/../yeti"
     */
    protected File sourceDir;

    /**
     * The directory in which to find test yeti source code
     *
     * @parameter expression="${project.build.testSourceDirectory}/../yeti"
     */
    protected File testSourceDir;

    @Override
    @SuppressWarnings("unchecked")
    protected void doExecute() throws Exception {
        Set<String> classpath = new HashSet<String>();
        addToClasspath(YETI_GROUPID, YETI_LIBRARY_ARTIFACTID, yetiVersion, classpath);
        addToClasspath(YETI_GROUPID, YETI_MAVEN_ARTIFACTID, yetiVersion, classpath);
        addToClasspath(YETI_GROUPID, YETICL_ARTIFACTID, yetiVersion, classpath);
        addToClasspath("jline", "jline", "0.9.94", classpath);
        classpath.addAll(project.getCompileClasspathElements());

        if (useTestClasspath) {
            classpath.addAll(project.getTestClasspathElements());
        }
        if (useRuntimeClasspath) {
            classpath.addAll(project.getRuntimeClasspathElements());
        }
        String[] classPathUrls = classpath.toArray(new String[classpath.size()]);

        ClassLoader parentCl = null;
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
            parentCl = new URLClassLoader(urlsA,ClassLoader.getSystemClassLoader());
        }else{
            getLog().error("No classpath this must not happen");
            throw new IllegalStateException("No classpath here");
        }

        //set the pathes
        String[] srcPathes = null;
        {
            //for the main sourceDirs
            List<String> sources = project.getCompileSourceRoots();
            //Quick fix in case the user has not added the "add-source" goal.
            String tSourceDir = sourceDir.getCanonicalPath();
            if(!sources.contains(tSourceDir)) {
                sources.add(tSourceDir);
            }

            List<String> srcDirs = new ArrayList<String>(sources);

            if(this.useTestClasspath){
                sources = project.getTestCompileSourceRoots();
                tSourceDir = testSourceDir.getAbsolutePath();
                if(!sources.contains(tSourceDir)) {
                    sources.add(tSourceDir);
                }
                srcDirs.addAll(sources);
            }
            if(srcDirs.size() > 0) {
                srcPathes = srcDirs.toArray(new String[srcDirs.size()]);
            }
        }

        String[] cds = null;
        if(commands != null && commands.trim().length() != 0){
            cds = commands.split(";;");
            for(int i= 0;i<cds.length;i++) {
                cds[i] = cds[i].trim();
            }
        }else{
            cds = null;
        }
        getLog().info("commands: "+commands+" \n\nis:"+cds);
        //load the yetishillutils

        Method m = parentCl.loadClass("org.yeticl.YetiShellUtils").getMethod("evaluateLoop", ClassLoader.class,String[].class,String[].class);
        m.invoke(null, parentCl,srcPathes,cds);
    }
}
