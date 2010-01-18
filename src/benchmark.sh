#!/bin/bash

for i in $(seq 10 10 100)
do
 echo "Benchmarking $i"
 java -classpath build/classes:lib/commons-cli.jar:lib/log4j.jar edu.washington.cs.activedht.expt.JSMicrobenchmark $i $1
done
