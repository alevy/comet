#!/bin/bash

for i in $(seq 10 10 100)
do
 echo "Benchmarking $i"
 java -Djava.library.path=luajava  -classpath build/classes:lib/commons-cli.jar:lib/log4j.jar edu.washington.cs.activedht.expt.LuaMicrobenchmark $i $1 $2
done
