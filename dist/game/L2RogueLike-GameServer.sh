#! /bin/sh

# exit codes of GameServer:
#  0 normal shutdown
#  2 reboot attempt

TERM=vt220
JAVA_HOME="${1:-/opt/jdk-11.0.5}"
JDK_HOME="${JAVA_HOME}"
JAVAC="${JAVA_HOME}/bin/javac"
PATH=${JAVA_HOME}/bin:${PATH}
JAVA="${JAVA_HOME}/bin/java"
[ ! -x "${JAVA}" ] && printf "oops! no executable java in JAVA_HOME\n" && exit 1

function LogRotate () {
  [ ! -f "$1" ] && return 1
  dt=`date +%Y%m%d%H%M%S`
  bn=`basename "$1"`
  dn=`dirname "$1"`
  bzip2 < "$1" > "${dn}/${dt}_${bn}"
  :> "$1"
}

#while :; do
	LogRotate log/java0.log
	LogRotate log/stdout.log
	exec ${JAVA} -Xms1g -Xmx4g -Xmn512m -XX:SurvivorRatio=8 -XX:+UseParallelGC -XX:NewRatio=3 -jar l2jdevs.jar >> log/stdout.log 2>&1
#	[ $? -ne 2 ] && break
#	sleep 10
#done

