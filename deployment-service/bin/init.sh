#!/bin/sh

# Set ACT_PLATFORM_* environment variables to override default directories.
CONFDIR="${ACT_PLATFORM_CONFDIR:-conf}"
LOGDIR="${ACT_PLATFORM_LOGDIR:-logs}"

# Set ACT_PLATFORM_JAVA_OPTS environment variable to override default options.
JAVA_OPTS="${ACT_PLATFORM_JAVA_OPTS:--XX:-OmitStackTraceInFastThrow}"

# Define base directories which are part of the deployment package.
EXAMPLESDIR="examples"
LIBDIR="libraries"
RESOURCESDIR="resources"

# Define parameters for executing the application.
PROPERTIES="$CONFDIR/application.properties"
MAINCLASS="no.mnemonic.commons.container.BootStrap"
ARGS="guice module=no.mnemonic.act.platform.service.modules.TiServiceModule module=no.mnemonic.act.platform.service.modules.TiServerModule"

STDOUT_FILE="$LOGDIR/stdout.log"
STDERR_FILE="$LOGDIR/stderr.log"
PIDFILE="$LOGDIR/application.pid"

usage() {
  echo "Usage: $0 start       - Start application"
  echo "       $0 restart     - Restart application"
  echo "       $0 stop        - Stop application"
  echo "       $0 status      - Print application status"
}

ensure_cassandra_running() {
   CASSANDRA_HOSTS=$(grep -Eo 'cassandra.contact.points=[^,]+' $PROPERTIES | sed 's/.*=//' | sed -E 's/,\s*/ /g')
   CASSANDRA_PORT=$(grep -Eo 'cassandra.port=.+' $PROPERTIES | sed 's/.*=//')
   MAX_RETRIES=30
   retry=0

   # Return if netcat is not found
   which nc > /dev/null 2> /dev/null || return

   while [ $retry -lt $MAX_RETRIES ]
   do
       for CASSANDRA_HOST in $CASSANDRA_HOSTS
       do
           nc -z $CASSANDRA_HOST $CASSANDRA_PORT
           if [ $? = 0 ]
           then
               echo "Cassandra is running at $CASSANDRA_HOST:$CASSANDRA_PORT"
               return
           fi
           echo "Cassandra is not available at $CASSANDRA_HOST:$CASSANDRA_PORT, will retry..."
           sleep 5
       done
   done
   echo "Cassandra is not available" 2> /dev/null
   exit 1
}

ensure_elasticsearch_running() {
   # Return if curl is not found. Check will not be executed.
   which curl > /dev/null 2> /dev/null || return

   ES_HOSTS=$(grep -Eo 'elasticsearch.contact.points=[^,]+' $PROPERTIES | sed 's/.*=//' | sed -E 's/,\s*/ /g')
   ES_PORT=$(grep -Eo 'elasticsearch.port=.+' $PROPERTIES | sed 's/.*=//')

   MAX_RETRIES=30
   retry=0
   while [ $retry -lt $MAX_RETRIES ]
   do
       for ES_HOST in $ES_HOSTS
       do
           curl --silent \
               "http://$ES_HOST:$ES_PORT/_cluster/health?wait_for_status=yellow&timeout=5s" \
               | fgrep '"timed_out":false' \
               > /dev/null
           if [ $? = 0 ]
           then
               # Elasticsearch is available and cluster is yellow or green
               echo "Elasticsearch is running at $ES_HOST:$ES_PORT"
               return
           fi
           echo "Elasticsearch at $ES_HOST:$ES_PORT is not up or cluster is red. Will retry..."
       done
       sleep 5
       retry=$(expr $retry + 1)
   done
   echo "Elasticsearch is not available" 2> /dev/null
   exit 2
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

  # Make sure elasticsearch and cassndra is running
  ensure_cassandra_running
  ensure_elasticsearch_running

  # Start application and pipe output into log files.
  java $JAVA_OPTS -Dapplication.properties.file=$PROPERTIES -cp $CLASSPATH $MAINCLASS $ARGS 1>> $STDOUT_FILE 2>> $STDERR_FILE &
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
