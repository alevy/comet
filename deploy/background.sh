#!/bin/bash
cd $1
echo $2
nohup $2 &> `hostname`.out < /dev/null &
echo $! > `hostname`.pid