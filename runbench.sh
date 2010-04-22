#!/bin/bash

for i in 1 10 100 1000 10000 100000 1000000
do
  ./benchmark.sh `cat ~/bigraid/activedht/data/scalability/nodethrough/lua.conf ` -f ~/bigraid/activedht/expons/$i.csv -o ~/bigraid/activedht/data/scalability/nodethrough/$i.csv
done
