Welcome to the yeti-maven-plugin plugin for Apache Maven 2.

This plugin has been designed to make working with yeti as easy as possible, when working in a
mixed language, enterprise project.

The code of this plugin is heavily based on the mavan-scala-plugin. Thanks to the maven-scala-plugin team.

## Available goals

 * yeti:compile
 * yeti:compile-pre-test
 * yeti:testCompile
 * yeti:console
 * yeti:doc
 * yeti:add-source
 
## Getting started with Yeti and Maven

To use this plugin and start working with yeit, start with a blank maven project and declare the plugin and
add a dependency on yeti in the maven pom.xml file:

	<?xml version="1.0" encoding="UTF-8"?>
	<project xmlns ...>
		....
		<repositories>
			...
			<repository>
				<id>chrisis.snapshots</id>
				<name>chrisis-maven-repo</name>
				<url>http://chrisichris.github.com/chrisis-maven-repo/snapshots</url>
			</repository>
		</repositories>
		<pluginRepositories>
			....
			<pluginRepository>
				<id>chrisis-plugin.snapshots</id>
				<name>chrisis-plugin-maven-repo</name>
				<url>http://chrisichris.github.com/chrisis-maven-repo/snapshots</url>
			</pluginRepository>
		</pluginRepositories>-->
	   ....
		<plugins>
		  <plugin>
			<groupId>org.yeti</groupId>
			<artifactId>yeti-maven-plugin</artifactId>
			<version>0.1-SNAPSHOT</version>
                <executions>
                    <execution>
                        <goals>
							<!-- what the goals do see below -->
                            <goal>add-source</goal> <!-- include all *.yeti main sources ie in src/main/yeti in the generated artifact as resources -->
                            <goal>compile-pre-test</goal><!-- compile all *.yeti main sources in the generate-test-sources maven phase
							alternatively:
							<goal>compile</goal> compile all *.yeti main sources in the compile maven phase-->
                            <goal>testCompile</goal><!-- compile all *.yeti test sources in the test-compile maven phase -->
                        </goals>
                    </execution>
                </executions>
		  </plugin>
		</plugins>
		....
		<reporting>
			.....
			<plugins>
				<plugin>
					<groupId>org.yeti</groupId>
					<artifactId>yeti-maven-plugin</artifactId>
					<version>0.1-SNAPSHOT</version>
				</plugin>
			</plugins>
		</reporting>
		<dependencies>
		  <dependency>
			<groupId>org.yeti</groupId>
			<artifactId>yeti</artifactId>
			<version>0.1-SNAPSHOT</version>
		  </dependency>
		</dependencie>
		....
	</project>

This configuration will compile *.yeti main sources in the generate-test-sources phase, compile *.yeti test-source in the compile phase
add all *.yeti main sources to the resources, generate yetidoc.

Without any further configuration, Maven will compile any yeti modules you include in ./src/main/yeti/**/*.yeti, ./src/main/java/**/*.yeti
and ./src/test/yeti/**/*.yeti and ./src/test/yeti/**/*.yeti.

## Compiling in the compile phase or later right before the test-phase

You can either compile the yeti sources in the compile phase or later in the generate-test-sources (which is executed after the
compile phase but before the test-compile phase).

The difference comes mainly wehn you use the console (which is important for a dynamic development style).

If you compile in the compile phase yeti sources are compiled to class files at the same time as java files are compiled. 
If you you start now the console the console will load the modules from the class files and can not pick up changes to the sources.

If you compile the yeti sources in the later generate-test-sources phase. Than you can still start the compile phase before
invoking the console to compile the java sources however the yeti modules will not be compiled and so the console will pick 
them up from their sources and so you can change the sources und refresh the console without the need to restart the console and 
recompile - which is much faster.


## The console for Interactive Coding

Through the yeti:console goal you can start the console. By default the console contains the compile, test and runtime classpath.

The console will also load *.yeti module source files from the sourcedirectories if they have not been compiled to classes before.

## Including source files in the output directory

The plugin can also copy source files to the output directory.

Use the yeti:add-source goal to add all *.yeti sources in the main source directories (ie ./src/main/java or ./src/main/yeti) 
to the output directory.

Enjoy.



## Support

Join the discussion mailing list at:

http://groups.google.com/group/yeti-lang
