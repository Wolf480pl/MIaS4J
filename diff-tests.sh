#!/bin/sh
SCRIPT=./run-tests.sh
if [ "$1" == "-d" ]; then
  SCRIPT=./debug-tests.sh
fi
$SCRIPT "$@" 2>&1 |diff -u testoutput.txt -
