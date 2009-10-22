init=9870

for i in {0..39}
do
  num=$(($init + $i))
  nohup java -cp activedht/src/lib/junit.jar:activedht/src/lib/commons-cli.jar:activedht/src/lib/javassist.jar:activedht/src/lib/log4j.jar:activedht/src/build/classes edu.washington.cs.activedht.expt.ActivePeer $num tabinet.cs.washington.edu:$init > activedht/log_${HOSTNAME}_${num}.out 2> activedht/log_${HOSTNAME}_${num}.err < /dev/null &

done

