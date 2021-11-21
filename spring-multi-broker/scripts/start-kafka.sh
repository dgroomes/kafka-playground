#!/usr/bin/env bash
# This is an unusual Kafka start script because it starts TWO brokers.
#
# Starts the brokers using KRaft mode (Kafka Raft). With KRaft, Zookeeper is not needed!
#
# Assumes that Kafka is installed. I installed Kafka with `brew install kafka`.
#
# TIP: Adjust log levels as needed using the "log4j.properties" file. Then look at the logs in "tmp-kafka/logs/". It will
# be a lot of information but with some determination it is an effective way to learn and experiment with Kafka!
#
# NOTE: This is not an idiomatic way to run Kafka. This was my best attempt to script out a way to run Kafka for local
# development.

set -ue
ATTEMPTS=3
# Get the script's containing directory. See https://stackoverflow.com/a/246128
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

# Start a fresh Kafka instance using KRaft. By "fresh", I mean "delete all the existing data"!
#
# In part, this function follows the steps outlined in the quick start guide https://github.com/apache/kafka/tree/2.8/config/kraft
startKafkaFresh() {
  # Delete existing data. This is a destructive operation! This script is only meant to be used for local development.
  rm -rf /tmp/kraft-combined-logs-a/*
  rm -rf /tmp/kraft-combined-logs-b/*

  # Generate a cluster ID
  local uuidA=$(kafka-storage random-uuid)
  local uuidB=$(kafka-storage random-uuid)

  # Format Storage Directories
  kafka-storage format -t "$uuidA" -c "$DIR/server-a.properties"
  kafka-storage format -t "$uuidB" -c "$DIR/server-b.properties"

  # Set up a temporary directory for Kafka to store its logs, data, etc. Delete everything that may already exist there
  # from previous instances of Kafka.
  mkdir -p "$DIR/tmp-kafka-a/"
  mkdir -p "$DIR/tmp-kafka-b/"
  rm -rf   "$DIR/tmp-kafka-a/"*
  rm -rf   "$DIR/tmp-kafka-b/"*
  mkdir -p "$DIR/tmp-kafka-a/logs"
  mkdir -p "$DIR/tmp-kafka-b/logs"

  export KAFKA_LOG4J_OPTS="-Dlog4j.configuration=file:$DIR/log4j.properties"

  {
    # Configure custom values
    export LOG_DIR="$DIR/tmp-kafka-a/logs"

    # Start the server!
    echo "Starting Kafka broker 'A'..."
    # Notice the "-daemon" flag. This is useful because it means the logs won't show up in the terminal.
    kafka-server-start -daemon "$DIR/server-a.properties"
  }

  {
    # Configure custom values
    export LOG_DIR="$DIR/tmp-kafka-b/logs"

    # Start the server!
    echo "Starting Kafka broker 'B'..."
    # Notice the "-daemon" flag. This is useful because it means the logs won't show up in the terminal.
    kafka-server-start -daemon "$DIR/server-b.properties"
  }
}

# Use kcat to check if Kafka is up and running. There is a timeout built in to the metadata query ('-L' command)
# of 5 seconds https://github.com/edenhill/kcat/issues/144
checkKafka() {
  kcat -L -b $BROKER_ORIGIN
}

waitForUp() {
  for i in $(seq 1 $ATTEMPTS) ; do
      if ((i > 1)); then
        echo "Checking if Kafka is up and running..."
      fi
      checkKafka &> /dev/null

      if [[ $? = 0 ]]; then
        # Change text output to bold. See https://stackoverflow.com/a/20983251
        tput bold
        echo "Kafka broker '$BROKER_NAME' is up and running at $BROKER_ORIGIN!"
        tput sgr0
        return
      fi
  done

  # Change text output color to red. See https://stackoverflow.com/a/20983251
  tput bold
  tput setaf 1
  echo >&2 "Gave up waiting for Kafka to be up and running!"
  tput sgr0
}

startKafkaFresh

BROKER_NAME=A BROKER_ORIGIN=localhost:9092 waitForUp

BROKER_NAME=B BROKER_ORIGIN=localhost:9192 waitForUp
