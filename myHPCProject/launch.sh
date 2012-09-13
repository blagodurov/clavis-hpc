#!/bin/bash
# please provide the correct path to your tools.jar below

BASEDIR=$(dirname $0)
cd $BASEDIR
BASEDIR=`pwd`
javac -cp ${BASEDIR}/samples-2.1.3.jar myHPCScheduling.java myHPCBestFit.java
cd ..
java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCProject/myHPCScheduling >> myHPCProject/log
cd $BASEDIR

