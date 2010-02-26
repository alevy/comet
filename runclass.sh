#!/bin/bash

classname=$1
shift

java -Djava.library.path=luajava  -classpath build/classes:lib/commons-cli.jar:lib/log4j.jar $classname $*
