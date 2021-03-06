#!/bin/sh

OUT_DIR=target/test-subjects
mkdir -p $OUT_DIR
echo "MakeTestMH" >&2
mvn-run-test com.github.wolf480pl.mias4j.MakeTestMH $OUT_DIR/TestMH.class
echo "MakeTestBuryUninitialized" >&2
mvn-run-test com.github.wolf480pl.mias4j.MakeTestBuryUninitialized $OUT_DIR/Test3.class
echo "MakeTestDynamic" >&2
mvn-run-test com.github.wolf480pl.mias4j.MakeTestDynamic $OUT_DIR/TestDynamic.class
