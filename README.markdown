Welcome to the yeti-maven-plugin plugin for Apache Maven 2
==========================================================

*Note: as of version 0.3 the old repl with auto-recompile etc is not supported anymore*

A maven-plugin for the yeti programming-language [yeti](http://mth.github.com/yeti/) 

The code of this plugin is heavily based on the mavan-scala-plugin. Thanks to the maven-scala-plugin team.

## Getting started with Yeti and Maven - the fastes way

The easiest way to get started is to git clone the yeti-maven-template-project.

Change update the pom with your project info and than run `mvn compile` to test
wheter everything is working.

## Direcotry Layout

The yeti sources go to `src/main/yeti` and `src/test/yeti`

## Available goals

 * yeti:compile
 * yeti:testCompile
 * yeti:repl
 * yeti:doc
 * yeti:add-source

The repl goal starts a yeti repl with the projects classpath set.

## Yeti jar

By default the plugin picks up the yeti-compiler from the compile-dependencies. 
Therefore the yeti.jar is needed in the dependencies. If you
do not want to have the yeti.jar but only the yeti-lib.jar in the dependencies 
(ie for Android develoment) set the plugin configuration `yetiLibOnly` 
to the yeti compiler version you want to use - the plugin will then load it 
down.

## Configuring the plugin

The only configuration ist `yetiLibOnly` see the pom.xml in the
template project for this configuration

## REPL for Interactive Coding

This features are not supported anymore

## Most important

Enjoy.

## Support

Join the discussion mailing list at:

http://groups.google.com/group/yeti-lang
