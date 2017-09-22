#!/bin/sh

EXECUTABLE="<path to jar file>"
PROPERTIES="<path to application.properties file>"
ARGS="<application arguments>"
JAVA_OPTS="-XX:-OmitStackTraceInFastThrow"

LOGDIR="<path to log directory>"
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
  # Check that executable file exists.
  if [ ! -f "$EXECUTABLE" ]; then
    echo "Executable file not found: $EXECUTABLE"
    exit 1
  fi

  # Check that properties file exists.
  if [ ! -f "$PROPERTIES" ]; then
    echo "Properties file not found: $PROPERTIES"
    exit 1
  fi

  # Ensure that log directory exists.
  if [ ! -d "$LOGDIR" ]; then
    mkdir "$LOGDIR"
  fi
}

# Start up application.
start() {
  # Ensure that application isn't already running.
  if [ -f "$PIDFILE" ]; then
    PID=`cat $PIDFILE`
    if [ -d /proc/$PID ]; then
      echo "Found an already running instance with pid: $PID"
      exit 1
    else
      # Remove obsolete pid file.
      rm "$PIDFILE"
    fi
  fi

  # Start application and pipe output into log files.
  java $JAVA_OPTS -Dapplication.properties.file="$PROPERTIES" -jar "$EXECUTABLE" $ARGS 1>> "$STDOUT_FILE" 2>> "$STDERR_FILE" &
  # Create PID file.
  echo $! > "$PIDFILE"

  echo "Application started."
}

# Shut down application.
stop() {
  if [ -f "$PIDFILE" ]; then
    PID=`cat $PIDFILE`
    if [ -d /proc/$PID ]; then
      # Shutdown application.
      kill $PID
      # Remove pid file.
      rm "$PIDFILE"
    fi
  fi

  echo "Application stopped."
}

# Check application status.
status() {
  if [ -f "$PIDFILE" ]; then
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