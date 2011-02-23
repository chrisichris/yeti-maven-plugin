Welcome to the yeti-maven-plugin plugin for Apache Maven 2
==========================================================

This plugin has been designed to make working with the programming-language [yeti](http://mth.github.com/yeti/) 
as easy as possible.

The code of this plugin is heavily based on the mavan-scala-plugin. Thanks to the maven-scala-plugin team.

## Available goals

 * yeti:compile
 * yeti:testCompile
 * yeti:repl
 * yeti:doc
 * yeti:add-source
 
## Getting started with Yeti and Maven - the detailed way

To use this plugin and start working with yeti, start with a blank maven project and declare the plugin and
add a dependency on yeti in the maven pom.xml file. (The yeti jar is not needed):

	<?xml version="1.0" encoding="UTF-8"?>
	<project xmlns ...>
		....

		<pluginRepositories>
			....
			<pluginRepository>
				<id>chrisis-plugin.snapshots</id>
				<name>chrisis-plugin-maven-repo</name>
				<url>http://chrisichris.github.com/chrisis-maven-repo/snapshots</url>
			</pluginRepository>
		</pluginRepositories>
		.....
		<build>
			<plugins>
				.....
				<plugin>
					<groupId>org.yeti</groupId>
					<artifactId>yeti-maven-plugin</artifactId>
					<version>0.1-SNAPSHOT</version>
					<executions>
						<execution>
							<goals>
								<goal>compile</goal>
								<goal>testCompile</goal>
							</goals>
						</execution>
					</executions>
				</plugin>
				.....
			</plugins>
			.....
		</build>
		....
	</project>
	
This does the following:

1.  add the repositorie for the yeti-plugin

3.  configure the yeti plugin to compile yeti source during the compile and test-compile phases. 	
	
You can put your yeti main sources in the `/src/main/java` and/or `/src/main/yeti` folders and 
the test-sources in `/src/test/yeti` and/or `/src/test/java folders`. 

`mvn compile` will compile them `mvn test-compile` will compile also the tests

With the `yeti-compile` system-property you can turn compilation off and on. This is important when
working with the repl because the repl can read (modified) sources directly if they are not compiled.


## REPL for Interactive Coding

With the yeti:repl goal you can start a console. 

If the yeti sources are not compiled, the sources are compiled by the repl on the fly, which means that 

- the repl can pick up changes to sources with out restart/recompilation which gives much fast development cycles

- and the repl does only compile those sources for which modules are explicitly loaded 
  If you have changed other sources they do not have to compile to test other sources which do not depend on them.
  
  This is very useful for continous-compilation - which automatically compiles yeti code once you save it (see below)

Therefore you will nearly always start the repl without compiling the yeti sources first (but still compile the java sources).

The best way to do this is with the `-Dyeti-compile=false` from the command line when invoking mvn:

	mvn -Dyeti-compile=false clean compile yeti:repl

This will clean and compile the project but without compiling yeti sources and it will start than the repl

### REPL classpath

By default the repl contains the compile, test and runtime classpath. 

You can control which classpaht is included with the 
useTestClasspath (-Dyeti.maven.repl.useTestClasspath=true) and useRuntimeClasspath (-Dyeti.maven.repl.useRuntimeClasspath) 
properties.

### Main and Shell REPL

The console you see when running the yeti:repl goal is actually used to control two different repls:

1.  the main repl is used for try-out/evalute code etc from your project. All commands entered at the prompt
	without a "-" prefix are evaluated in this repl. 

    There is nothing realy special about this repl. It is like a normal yeti repl.
	
	--help shows a help.

2.  the second repl the shell repl is more like a build repl from which you can ie reload the main repl if sources changed, specify
	the source diretories for the main repl, evaluate some code in an extra separate repl, monitor source changes to trigger tests etc

	To execute code in the second repl prefix the code expressions with "-"

Both repls are strictly seperated they use different classloaders for the srouce-modules and so do not share source-modules or state (but run in the same vm).
They can also not communicate. 	
	
Example:
	
	yeti>y=1+1 //this code is evaluated in the main repl
	yeti>-y=1+2 //this code is evalutaed in the shell repl
	yeti>println y
	2
	yeti>-println y
	3

### The shell REPL and continous compilation

The shell repl is more like a build repl from which you can ie reload the main repl if sources changed, specify
the source diretories for the main repl, evaluate some code in an extra repl, monitor source changes to trigger tests etc

To execute code in the shell repl prefix the code expressions with "-"

#### Continous Compilation

When typing yeti-code it is very handy if it gets countinously checked in the background when you save the source-file (this is simlar to
what ide do with java-code).

To do this you start the repl and ad a monitor to the shell and load the module you are currently developing

	mvn -Dyeti-compile=false clean compile yeti:repl
	....
	yeti>-s.addMonitor "recompile" \(s.branche "load fullQualifiedNameOfModuleYourWorkOn");

Here you first start the repl (note that the yeti sources are not compiled) and than you add a monitor to the shell. The monitor
gets executed each time a source-file changes. "recompile" is the name of the monitor the montior itself is a function and in this
case `s.branche "load fullQualifiedNameOfModuleYourWorkOn"` means that in a sperate repl the code given as string is executed. 
The load command will be executed in an extra seperated-repl.
The command will load the module and compile it on the fly (each time you change the sources).

To stop the monitor use `-s.showMonitors ()` this will print all current monitors with their name and id. Use the id
to call either `-s.activateMonitor id false` to temporarily deactivate the monitor or `-s.removeMonitor id` to remove the 
monitor. 

#### The shell module "s"

The shell repl contains a special preloaded module "s" (for shell). This module contains
different functions and variables which are useful during development. Ie to reload the first
repl if sources have changed type at the prompt:

	yeti>-s.refresh()

The shell module has the following fields:

- var sourceDirs is list<string> : this are the dirs of the yeti sources. From there the sources are loaded
but only if they are not compiled on the classpath. 

-   addMonitor is string -> (()->()) -> (): you can add a function which is triggered when the content of the sourceDirs change
the first argument is the name of the monitor the second the monitor. Each monitor is given automatically an id.
To see all monitor ids use showMonitors()
	
	The monitor function is executed on a different thread.

- removeMonitor is number -> (): remove the monitor with the given id number (to see the id use showMonitors())

- activateMonitor is number -> boolean -> (): instead of removing you can als temporaraly deactivate a monitor with
the given id-number: `-s.activateMonitor id false` and later reactivate with `-s.activateMonitor id true`.

- showMonitors is () -> () : prints a list of all monitors and their ids

- refresh : () -> () : discards the old main repl and loads a new one with the new sources and sourcedirs.
If you want to pick up changes to sources inthe main repl you have to call this method ie `-s.refresh()` 

- var refreshCode is string : here code can be set which is called each time in a new repl when refresh is
called. The code is called before user input is expected. 

- branche is string -> () : executes the given yeti-code string in a new (non interactive repl)
after the executions the new repl is closed again. The execution happens on the same thread.

- _start : do not call this method. This is for implementation only 

#### Executing predefined commands in the shell-repl before user-input

You can also specify in the maven pom.xml commands which are executed line by line in the shell-repl 
right after it is started but before user input is expected. 

This is useful ie to preload modules beside the shell module.

The commands are specified in the configuration of the yeti-maven-plugin in the `<commands>` tag. Ie in the maven pom.xml 

	<plugin>
		<groupId>org.yeti</groupId>
		...
		<configuration>
			<commands>
				ys = load org.yeb.yebshell;;
				s.sourceDirs := "some/Directory" :: s.sourceDirs;;
			</commands>
		</configuration>
	</plugin>
  
The above commands are excuted in the shell repl. Note that there is no trailing "-" needed nor allowed). Ending a command with ";;" means
that it will be executed on a new line in the shell repl

#### The shell and repl code itself

Note the code of the shell and repl is in the [yeticl project](https://github.com/chrisichris/yeticl) 

## Including source files in the output directory

The plugin can also copy source files to the output directory.

Use the yeti:add-source goal to add all *.yeti sources in the main source directories (ie ./src/main/java and ./src/main/yeti) 
to the output directory.

## Yetidoc 

To generate (rudimentary) api-documentation use the yeti:doc goal.

To include yetidoc in site-generation add to your pom.xml:

    <reporting>
        <plugins>
            <plugin>
                <groupId>org.yeti</groupId>
                <artifactId>yeti-maven-plugin</artifactId>
                <version>0.1-SNAPSHOT</version>
            </plugin>
        </plugins>
    </reporting>


Code for this api is found at the [yetidoc project](https://github.com/chrisichris/yetidoc)

## Most important

Enjoy.



## Support

Join the discussion mailing list at:

http://groups.google.com/group/yeti-lang
