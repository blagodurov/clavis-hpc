#!/bin/bash
# Please provide the correct path to your tools.jar below

# The input files are stored in the "inputs" directory. Please setup the correct path to their location in myHPCBranching/myHPCScheduling:
# 	String classes_filename = "/cs/systems/home/sba70/octopus_garden/clavis-src/classes-for-choco-" + options[13] + ".csv";
# 	String comm_filename = "/cs/systems/home/sba70/octopus_garden/clavis-src/comm-matrix-for-choco-" + options[13] + ".csv";
# 	String schedule_filename = "/cs/systems/home/sba70/octopus_garden/clavis-src/schedule-" + options[13] + ".csv";
# Please note that the "schedule_filename" is currently unused by the solver.

# The options to myHPCBranching/myHPCScheduling below (please refer to the paper for details):
# TIME_LIMIT_IN_MS: 60000
# SET_LOGGING: 0
# COMPUTE_RANDOM: 0
# d: 1
# p: 1
# c: 1
# USE_FUND_DISTINCT: 1
# USE_FUND_PRIO: 1
# USE_CONTAINER_REORDERING: 1
# USE_NAIVE_F: 0
# USE_POTENTIAL_F: 1
# DO_FATHOMING: 1
# PRUNE_TOP_PERC: 0
# RANKS_PER_JOB: 128

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
