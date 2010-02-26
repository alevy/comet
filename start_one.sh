init=9870

while [ -f continue_dht ]
do
  java -cp activedht/src/lib/junit.jar:activedht/src/lib/commons-cli.jar:activedht/src/lib/javassist.jar:activedht/src/lib/log4j.jar:activedht/src/build/classes edu.washington.cs.activedht.expt.ActivePeer tabinet.cs.washington.edu:$init
done

