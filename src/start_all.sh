init=9870

curdir=$PWD
cd $HOME

for ((i=0; i<$1; i++))
do
  nohup activedht/src/start_one.sh > activedht/logs/log_${HOSTNAME}_${num}.out 2> activedht/logs/log_${HOSTNAME}_${num}.err < /dev/null &

done

cd $curdir
