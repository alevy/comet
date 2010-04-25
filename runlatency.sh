#!/bin/bash

for i in 1 10 100 1000 10000 100000 1000000
do
  ./latency.sh `cat ~/bigraid/activedht/data/microbenchmarks/latency/lua.conf` -f ~/bigraid/activedht/expons/$i.csv -o ~/bigraid/activedht/data/microbenchmarks/latency/$i.csv
done
