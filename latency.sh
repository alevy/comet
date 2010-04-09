#!/bin/bash

for j in 1 8 100 500
do
  for i in 1 10 50 100 500 1000 5000
  do
    echo "Benchmarking $i,$j"
    `dirname $0`/runclass.sh edu.washington.cs.activedht.expt.LuaNodeLatencyMicrobenchmark -n $j -i $i $*
  done
done
