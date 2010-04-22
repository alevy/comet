#!/bin/bash

for i in 1 8 $(seq 10 10 100)
do
 echo "Benchmarking $i"
 `dirname $0`/runclass.sh edu.washington.cs.activedht.expt.LuaNodeLatencyMicrobenchmark -n $i $*
done
