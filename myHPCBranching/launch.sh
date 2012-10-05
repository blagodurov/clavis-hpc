#!/bin/bash
# please provide the correct path to your tools.jar below

BASEDIR=$(dirname $0)
cd $BASEDIR
BASEDIR=`pwd`
javac -cp ${BASEDIR}/samples-2.1.3.jar myHPCScheduling.java myImpactBasedBranching.java SolutionThread.java
cd ..
java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   1 1 100   1 1 1 0 1 1 1 12 >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
cd $BASEDIR
