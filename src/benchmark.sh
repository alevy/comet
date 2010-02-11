#!/bin/bash

for i in $(seq 10 10 100)
do
 echo "Benchmarking $i"
 ssh nethack.cs.washington.edu "screen -d -m workspace/activedht/src/runclass edu.washington.cs.activedht.expt.ActivePeer -h nethack.cs.washington.edu -p 4321 -b nethack.cs.washington.edu:4321"
 `dirname $0`/runclass.sh edu.washington.cs.activedht.expt.LuaMicrobenchmark -n $i -h `hostname -f` -p 1234 -b nethack.cs.washington.edu:4321 $*
 ssh nethack.cs.washington.edu "killall java"
done
