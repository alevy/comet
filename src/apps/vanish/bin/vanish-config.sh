#
# Script is included in all the Vanish scripts using the source command.
# Script is taken from Hadoop.
#
# Should not be executable directly; Should not be passed any arguments,
# since we need original $*. 


# Resolve links - $0 may be a softlink.
this="$0"
while [ -h "$this" ]; do
  ls=`ls -ld "$this"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    this="$link"
  else
    this=`dirname "$this"`/"$link"
  fi
done

# Convert relative path to absolute path.
bin=`dirname "$this"`
script=`basename "$this"`
bin=`cd "$bin"; pwd`
this="$bin/$script"

# The root of the vanish installation.
export VANISH_HOME=`dirname "$this"`/..

# Check to see if the conf dir or vanish home are given as an optional
# arguments.
while [ $# -gt 1 ]
do
  if [ "--config" = "$1" ]
  then
    shift
    confdir=$1
    shift
    VANISH_CONF_DIR=$confdir
  elif [ "--service" = "$1" ]
  then
    shift
    service_host=$1
    shift
    VANISH_SERVICE=$service_host
  else
    # Presume we are at end of options and break.
    break
  fi
done
 
# Allow alternate vanish conf dir location.
VANISH_CONF_DIR="${VANISH_CONF_DIR:-$VANISH_HOME/conf}"
# List of vanish servers.
VANISH_SERVICE="${VANISH_SERVICE:-$VANISH_CONF_DIR/service}"

