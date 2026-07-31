#!/bin/sh

# Maven Wrapper shell script
# Apache Maven Wrapper 3.3.2

# ----------------------------------------------------------------------------
# Maven Wrapper startup script
# ----------------------------------------------------------------------------

# Resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' >/dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")/"$link"
  fi
done

# Get standard environment variables
PRGDIR=$(dirname "$PRG")

# Only set MAVEN_PROJECTBASEDIR if not already set
[ -z "$MAVEN_PROJECTBASEDIR" ] && MAVEN_PROJECTBASEDIR=$(cd "$PRGDIR" >/dev/null && pwd)

if [ -z "$MAVEN_HOME" ] || [ ! -e "$MAVEN_HOME/bin/mvn" ]; then
  if [ -z "$MAVEN_USER_HOME" ]; then
    MAVEN_USER_HOME="$HOME/.m2"
  fi

  WRAPPER_JAR="$MAVEN_USER_HOME/wrapper/dists/maven-wrapper-3.3.2.jar"

  if [ ! -f "$WRAPPER_JAR" ]; then
    WRAPPER_DIR="$MAVEN_USER_HOME/wrapper/dists"
    mkdir -p "$WRAPPER_DIR"
    WRAPPER_URL=$(grep 'wrapperUrl=' "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties" | cut -d= -f2)
    DISTRIBUTION_URL=$(grep 'distributionUrl=' "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties" | cut -d= -f2)

    if command -v curl >/dev/null 2>&1; then
      echo "Downloading Maven Wrapper..."
      curl -fsSL -o "$WRAPPER_JAR" "$WRAPPER_URL" || true
    elif command -v wget >/dev/null 2>&1; then
      echo "Downloading Maven Wrapper..."
      wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL" || true
    fi
  fi

  if [ -f "$WRAPPER_JAR" ]; then
    MAVEN_HOME="$MAVEN_USER_HOME/wrapper/dists"
  fi
fi

# JVM config
[ -f "$MAVEN_PROJECTBASEDIR/.mvn/jvm.config" ] && JVM_CONFIG=$(cat "$MAVEN_PROJECTBASEDIR/.mvn/jvm.config" | tr '\n' ' ')

# Maven opts
MAVEN_OPTS="${JVM_CONFIG} ${MAVEN_OPTS}"

export MAVEN_HOME
export MAVEN_PROJECTBASEDIR
export MAVEN_OPTS

if [ -n "$MAVEN_HOME" ] && [ -f "$MAVEN_HOME/bin/mvn" ]; then
  MVN_CMD="$MAVEN_HOME/bin/mvn"
else
  MVN_CMD="mvn"
fi

exec $MVN_CMD "$@"
