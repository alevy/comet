#!/bin/bash
cd $1
pid=`cat $HOSTNAME.pid`
kill $pid
