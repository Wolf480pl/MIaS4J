#!/bin/sh
#mvn-run-test -Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y com.github.wolf480pl.mias4j.Wrap $1 example/target/classes/ Test1 "$@"
ID=$1
shift
./run-test.sh $ID -d "$@"
