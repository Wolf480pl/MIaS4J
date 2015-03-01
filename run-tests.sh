#!/bin/sh

SCRIPT="./run-test.sh"

if [ -z $JAVA ]; then
  JAVA=java
fi

JAVA8_AVAIL=false

$JAVA -version 2>&1 |head -n1 |grep -P 'version \"1\.8'
if [ $? = 0 ]; then
  JAVA8_AVAIL=true
fi


for i in 1 2 3 4 ; do
  echo "----- TEST $i -----"
  
  if [ $i = 4 ] && ! $JAVA8_AVAIL; then
    echo "SKIPPED: no java 8 available";
  else
  
    $SCRIPT $i "$@"
    
  fi
done
