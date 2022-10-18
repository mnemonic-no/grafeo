#!/bin/sh

# Set ACT_PLATFORM_* environment variables to override default directories.
CONFDIR="${ACT_PLATFORM_CONFDIR:-conf}"
LOGDIR="${ACT_PLATFORM_LOGDIR:-logs}"

# Set ACT_PLATFORM_JAVA_OPTS environment variable to override default options.
JAVA_OPTS="${ACT_PLATFORM_JAVA_OPTS:--XX:-OmitStackTraceInFastThrow}"
# With Java versions newer than JDK8 add the required configuration for the Java module system.
JAVA_JPMS_OPTS="--add-modules java.se \
                --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
                --add-opens java.base/java.lang=ALL-UNNAMED \
                --add-opens java.base/java.nio=ALL-UNNAMED \
                --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
                --add-opens java.management/sun.management=ALL-UNNAMED \
                --add-opens jdk.management/com.ibm.lang.management.internal=ALL-UNNAMED \
                --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED \
                --add-opens java.base/java.util=ALL-UNNAMED"

# Define base directories which are part of the deployment package.
EXAMPLESDIR="examples"
LIBDIR="libraries"
RESOURCESDIR="resources"

# Define parameters for executing the application.
PROPERTIES="$CONFDIR/application.properties"
MAINCLASS="no.mnemonic.commons.container.BootStrap"
ARGS="guice module=no.mnemonic.act.platform.rest.modules.TiRestModule module=no.mnemonic.act.platform.rest.modules.TiClientModule"

STDOUT_FILE="$LOGDIR/stdout.log"
STDERR_FILE="$LOGDIR/stderr.log"
PIDFILE="$LOGDIR/application.pid"

usage() {
  echo "Usage: $0 start       - Start application"
  echo "       $0 restart     - Restart application"
  echo "       $0 stop        - Stop application"
  echo "       $0 status      - Print application status"
}

# Set up everything this script needs.
setup() {
  # Copy example configuration to configuration folder on first execution.
  if [ ! -d $CONFDIR ] && [ -d $EXAMPLESDIR ]; then
    mkdir -p $CONFDIR
    cp $EXAMPLESDIR/* $CONFDIR
  fi

  # Ensure that log directory exists.
  if [ ! -d $LOGDIR ]; then
    mkdir -p $LOGDIR
  fi

  # Check that library directory exists.
  if [ ! -d $LIBDIR ]; then
    echo "Library directory not found: $LIBDIR"
    exit 1
  fi

  # Check that resources directory exists.
  if [ ! -d $RESOURCESDIR ]; then
    echo "Resources directory not found: $RESOURCESDIR"
    exit 1
  fi

  # Check that properties file exists.
  if [ ! -f $PROPERTIES ]; then
    echo "Properties file not found: $PROPERTIES"
    exit 1
  fi
}

# Start up application.
start() {
  # Ensure that application isn't already running.
  if [ -f $PIDFILE ]; then
    PID=`cat $PIDFILE`
    if [ -d /proc/$PID ]; then
      echo "Found an already running instance with pid: $PID"
      exit 1
    else
      # Remove obsolete pid file.
      rm $PIDFILE
    fi
  fi

  # Construct classpath with all libraries and additional resources.
  CLASSPATH="$RESOURCESDIR"
  for jar in `ls $LIBDIR/*.jar`; do
    CLASSPATH="$CLASSPATH:$jar"
  done

  JAVA_BIN=`which java`
  if [[ -n $JAVA_HOME ]]; then
    # Respect JAVA_HOME environment variable if present.
    JAVA_BIN=$JAVA_HOME/bin/java
  fi

  # Check that Java executable exists.
  if [[ ! -x $JAVA_BIN ]]; then
    echo "Java executable not found: $JAVA_BIN"
    exit 1
  fi

  JAVA_VERSION=`$JAVA_BIN -version 2>&1 | grep -i version | cut -d '"' -f 2`
  if [[ $JAVA_VERSION == 1.8* ]]; then
    # With JDK8 unset this variable because the parameters are not supported by the JVM (and not needed).
    JAVA_JPMS_OPTS=""
  fi

  echo "Executing application on Java version $JAVA_VERSION."
  # Start application and pipe output into log files.
  $JAVA_BIN $JAVA_OPTS $JAVA_JPMS_OPTS -Dapplication.properties.file=$PROPERTIES -cp $CLASSPATH $MAINCLASS $ARGS 1>> $STDOUT_FILE 2>> $STDERR_FILE &
  # Create PID file.
  echo $! > $PIDFILE

  echo "Application started."
}

# Shut down application.
stop() {
  if [ -f $PIDFILE ]; then
    PID=`cat $PIDFILE`
    if [ -d /proc/$PID ]; then
      # Shutdown application.
      kill $PID
      # Remove pid file.
      rm $PIDFILE
    fi
  fi

  echo "Application stopped."
}

# Check application status.
status() {
  if [ -f $PIDFILE ]; then
    PID=`cat $PIDFILE`
    if [ -d /proc/$PID ]; then
      echo "Application is running."
    else
      echo "Application is not running."
    fi
  else
    echo "Application is not running."
  fi
}

# Change into the parent directory of this script (for correct relative paths).
cd `dirname $0`/..

# Execute setup() first.
setup

# Execute specified command afterwards.
case $1 in
  start)
    start
  ;;
  restart)
    stop
    start
  ;;
  stop)
    stop
  ;;
  status)
    status
  ;;
  *)
    usage
  ;;
esac