#!/usr/bin/env bash

# Start the Vanish daemon. Run the script on the Vanish node.
# Script taken from Hadoop.
#
USAGE="USAVE: start-vanish.sh [<service params>]"

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

. "$bin"/vanish-config.sh

errCode=$?
if [ $errCode -ne 0 ]
then
  exit $errCode
fi

"$bin"/vanish-daemon.sh --config "${VANISH_CONF_DIR}" start service "$@"
