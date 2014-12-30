#!/bin/sh
mvn-run-test -Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y com.github.wolf480pl.sandbox.Wrap $1 example/target/classes/ Test1 "$@"
