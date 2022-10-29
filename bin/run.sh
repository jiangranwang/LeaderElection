#!/usr/bin/env sh

CLASSPATH="../target/leaderElection-1.0-SNAPSHOT-shaded.jar"
LOGGING="-Djava.util.logging.config.file=./logging.properties"
USER_HOME="-Duser.home=$(pwd)"
DEBUG=""

java ${DEBUG} -cp ${CLASSPATH} ${LOGGING} ${USER_HOME} election.Simulator $@
