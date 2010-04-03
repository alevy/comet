#!/bin/bash

classname=$1
shift

#java -classpath build/classes:lib/commons-cli.jar:lib/log4j.jar $classname $*
java -classpath dist/comet.jar:lib/Azureus2.jar $classname $*
