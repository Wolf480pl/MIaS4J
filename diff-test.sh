#!/bin/sh
SCRIPT=./run-test.sh
#if [ "$1" == "-d" ]; then
#  SCRIPT=./debug-tests.sh
#fi

TEST_ID=$1;

$SCRIPT "$@" 2>&1 |diff -u test${TEST_ID}output.txt -
