#!/usr/bin/env bash
# 
# Runs a Vanish command as a daemon.
#
# Taken from Hadoop.
#
# Environment Variables
#
#   VANISH_CONF_DIR   Alternate vanish conf dir. Default is ${VANISH_HOME}/conf.
#   VANISH_LOG_DIR    Where log files are stored.  PWD by default.
#   VANISH_PID_DIR    The pid files are stored. /tmp by default.
#   VANISH_USER_ID   A string representing this instance of hadoop. $USER by default
#   VANISH_NICENESS The scheduling priority for daemons. Defaults to 0.
#
# Modelled after $HADOOP_HOME/bin/hadoop-daemon.sh

usage="Usage: vanish-daemon.sh [--config <conf-dir>]\
 (start|stop) <vanish-command> \
 <args...>"

# If no args specified, show usage.
if [ $# -le 1 ]; then
  echo $usage
  exit 1
fi

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

. "$bin"/vanish-config.sh

# Get arguments.
startStop=$1
shift

command=$1
shift

vanish_rotate_log ()
{
    log=$1;
    num=5;
    if [ -n "$2" ]; then
    num=$2
    fi
    if [ -f "$log" ]; then  # rotate logs
    while [ $num -gt 1 ]; do
        prev=`expr $num - 1`
        [ -f "$log.$prev" ] && mv "$log.$prev" "$log.$num"
        num=$prev
    done
    mv "$log" "$log.$num";
    fi
}

if [ -f "${VANISH_CONF_DIR}/vanish-env.sh" ]; then
  . "${VANISH_CONF_DIR}/vanish-env.sh"
fi

# Get log directory.
if [ "$VANISH_LOG_DIR" = "" ]; then
  export VANISH_LOG_DIR="$VANISH_HOME/logs"
fi
mkdir -p "$VANISH_LOG_DIR"

if [ "$VANISH_PID_DIR" = "" ]; then
  VANISH_PID_DIR=/tmp
fi

if [ "$VANISH_USER_ID" = "" ]; then
  export VANISH_USER_ID="0"
fi

# Some variables:

# Work out java location so can print version into log.
if [ "$JAVA_HOME" != "" ]; then
  #echo "run java in $JAVA_HOME"
  JAVA_HOME=$JAVA_HOME
fi
if [ "$JAVA_HOME" = "" ]; then
  echo "Error: JAVA_HOME is not set."
  exit 1
fi
JAVA=$JAVA_HOME/bin/java
export VANISH_LOGFILE=vanish-$VANISH_USER_ID-$command-$HOSTNAME.log
logout=$VANISH_LOG_DIR/vanish-$VANISH_USER_ID-$command-$HOSTNAME.out  
loglog="${VANISH_LOG_DIR}/${VANISH_LOGFILE}"
pid=$VANISH_PID_DIR/vanish-$VANISH_USER_ID-$command.pid

# Set default scheduling priority
if [ "$VANISH_NICENESS" = "" ]; then
    export VANISH_NICENESS=0
fi

case $startStop in

  (start)
    mkdir -p "$VANISH_PID_DIR"
    if [ -f $pid ]; then
      if kill -0 `cat $pid` > /dev/null 2>&1; then
        echo $command running as process `cat $pid`.  Stop it first.
        exit 1
      fi
    fi

    vanish_rotate_log $logout

    echo starting $command, logging to $logout

    # Add to the command log file vital stats on our environment.
    echo "`date` Starting $command on `hostname`" >> $loglog
    echo "ulimit -n `ulimit -n`" >> $loglog 2>&1

    nohup nice -n $VANISH_NICENESS "$VANISH_HOME"/bin/vanish \
        --config "${VANISH_CONF_DIR}" \
        $command "$@" > "$logout" 2>&1 < /dev/null &
    echo $! > $pid
    sleep 1; head "$logout"
    ;;

  (stop)
    if [ -f $pid ]; then
      if kill -0 `cat $pid` > /dev/null 2>&1; then  # TODO(roxana): support kill -0
        echo -n "Stopping $command"
        echo "`date` Stopping $command" >> $loglog
        while kill -9 `cat $pid` > /dev/null 2>&1; do
          echo -n "."
          sleep 1;
        done
        echo
      else
        echo no $command to stop
      fi
    else
      echo no $command to stop
    fi
    ;;

  (*)
    echo $usage
    exit 1
    ;;

esac
