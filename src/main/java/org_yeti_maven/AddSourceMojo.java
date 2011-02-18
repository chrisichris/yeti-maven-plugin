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
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 * Inlcude yeti sources in the result and add yeti source directories to the POM.
 *
 * @executionStrategy always
 * @goal add-source
 * @phase initialize
 * @requiresDirectInvocation false
 */
public class AddSourceMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
    * Helper class to assist in attaching artifacts to the project instance.
    * project-helper instance, used to make addition of resources simpler.
    * @component
    * @required
    * @readonly
    */
    private MavenProjectHelper projectHelper;

    /**
     * Wheter to include the .yeti files in the sourceDirectories as resources
     * in the build
     *
     * @parameter expression="${includeYetiSource}"
     *            default-value="true"
     */
    protected boolean includeYetiSource = true;

    /**
     * The directory in which yeti source is found
     *
     * @parameter expression="${project.build.sourceDirectory}/../yeti"
     */
    protected File sourceDir;

    /**
     * The directory in which java source is found
     *
     * @parameter expression="${project.build.sourceDirectory}"
     */
    protected File javaSourceDir;

    /**
     * The directory in which testing yeti source is found
     *
     * @parameter expression="${project.build.testSourceDirectory}/../yeti"
     */
    protected File testSourceDir;

    public void execute() throws MojoExecutionException {
        try {
            if (sourceDir != null) {
                String path = sourceDir.getCanonicalPath();
                if (!project.getCompileSourceRoots().contains(path)) {
                    getLog().info("Add Source directory: " + path);
                    project.addCompileSourceRoot(path);
                }
            }
            if (testSourceDir != null) {
                String path = testSourceDir.getCanonicalPath();
                if (!project.getTestCompileSourceRoots().contains(path)) {
                    getLog().info("Add Test Source directory: " + path);
                    project.addTestCompileSourceRoot(path);
                }
            }
            //according to http://www.maestrodev.com/better-builds-with-maven/accessing-project-sources-and-resources
            if(includeYetiSource) {
                List<String> sources = project.getCompileSourceRoots();

                List includes = Arrays.asList("**/*.yeti","**/*.teti");
                for(String path:sources) {
                    projectHelper.addResource(project,path,includes,null);
                }

            }
        } catch(Exception exc) {
            getLog().warn(exc);
        }
    }
}
