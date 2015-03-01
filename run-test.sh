#!/bin/sh

if [ "-d" = "$1" ]; then
  DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y"
  shift
fi

TEST_ID=$1
shift

CMD="mvn-run-test $DEBUG com.github.wolf480pl.sandbox.Wrap $1"

case $TEST_ID in
  1)
    $CMD example/target/classes/ Test1
    ;;
  2)
    $CMD target/test-subjects/ TestMH
    ;;
  3)
    $CMD target/test-subjects/ Test3
    ;;
  4)
    $CMD target/test-subjects/ TestDynamic
    ;;
  *)
    echo "Unknown test ID: $TEST_ID" >&2
    exit 1
esac
