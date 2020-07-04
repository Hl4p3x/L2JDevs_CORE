#! /bin/sh

TERM=vt220
JAVA_HOME="${1:-/opt/jdk-11.0.2}"
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

err=1
until [ ${err} == 0 ]; do
        LogRotate log/java0.log
        LogRotate log/stdout.log
	#exec ${JAVA} -Xms128m -Xmx256m -cp ./../libs/*:l2jlogin.jar org.l2jdevs.loginserver.L2LoginServer >> log/stdout.log 2>&1
	${JAVA} -Xms128m -Xmx256m -cp ./../libs/*:l2jlogin.jar org.l2jdevs.loginserver.L2LoginServer >> log/stdout.log 2>&1
	err=$?
	sleep 10
done

