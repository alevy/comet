init=9870

curdir=$PWD
cd $HOME

for ((i=0; i<$1; i++))
do
  nohup java -cp activedht/src/lib/junit.jar:activedht/src/lib/commons-cli.jar:activedht/src/lib/javassist.jar:activedht/src/lib/log4j.jar:activedht/src/build/classes edu.washington.cs.activedht.expt.ActivePeer tabinet.cs.washington.edu:$init > activedht/logs/log_${HOSTNAME}_${num}.out 2> activedht/logs/log_${HOSTNAME}_${num}.err < /dev/null &

done

cd $curdir
