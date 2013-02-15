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
        //classpath
		Set<String> classpath = new HashSet<String>();
        classpath.addAll(project.getCompileClasspathElements());

        if (useTestClasspath) {
            classpath.addAll(project.getTestClasspathElements());
        }
        if (useRuntimeClasspath) {
            classpath.addAll(project.getRuntimeClasspathElements());
        }

		invokeYeti(classpath, new String[]{});	
    }
}
