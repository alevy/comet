#!/usr/bin/env bash
#
# Stop Vanish daemon. Run this on Vanish service machine.
# Taken from Hadoop.
#

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

. "$bin"/vanish-config.sh

"$bin"/vanish-daemon.sh --config "${VANISH_CONF_DIR}" stop service
