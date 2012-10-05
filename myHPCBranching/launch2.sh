#!/bin/bash
# please provide the correct path to your tools.jar below

BASEDIR=$(dirname $0)
cd $BASEDIR
BASEDIR=`pwd`
javac -cp ${BASEDIR}/samples-2.1.3.jar myHPCScheduling.java myImpactBasedBranching.java SolutionThread.java
cd ..

for weights in "1 10 80" "1 80 10" "80 10 1" "10 80 1" "10 1 80" "80 1 10"
do
	for ranks in 12 24 48 64 128
	do
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 1   $weights   0 0 0 0 0 0 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   0 0 0 0 0 0 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 0 0 0 0 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 0 0 0 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 1 0 0 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 0 1 0 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 0 1 1 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 0 1 1 1 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 0 1 1 5 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 0 1 1 10 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
	done
done

for weights in "1 10 90" "1 90 10" "90 10 1" "10 90 1" "10 1 90" "90 1 10"
do
	for ranks in 12 24 48 64 128
	do
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 1   $weights   0 0 0 0 0 0 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   0 0 0 0 0 0 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 0 0 0 0 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 0 0 0 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 1 0 0 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 0 1 0 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 0 1 1 0 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 0 1 1 1 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 0 1 1 5 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
		java -cp $CLASSPATH:${BASEDIR}/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCBranching/myHPCScheduling 60000 0 0   $weights   1 1 1 0 1 1 10 $ranks >> myHPCBranching/log.txt 2>> myHPCBranching/log.txt
	done
done

cd $BASEDIR
