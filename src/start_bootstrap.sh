init=9870

curdir=$PWD

cd $HOME

nohup java -cp activedht/src/lib/junit.jar:activedht/src/lib/commons-cli.jar:activedht/src/lib/javassist.jar:activedht/src/lib/log4j.jar:activedht/src/build/classes edu.washington.cs.activedht.expt.ActivePeer -b tabinet.cs.washington.edu:$init $init > activedht/logs/log_${HOSTNAME}_${num}.out 2> activedht/logs/log_${HOSTNAME}_${num}.err < /dev/null &

cd $curdir
