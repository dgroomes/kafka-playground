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

set -eu

ATTEMPTS=3

# Move to the directory containing this script so that the rest of this script can safely assume that the current working
# directory is the containing directory. This is especially important because the 'server.properties' file specifies
# the property 'log.dirs=tmp-kafka-data-logs' which is relative to the current working directory.
cd -- "$( dirname -- "${BASH_SOURCE[0]}" )"

preconditions() {
  if ! which kcat &> /dev/null; then
    echo >&2 "The 'kcat' command was not found. Please install kcat."
    exit 1
  fi
  if ! which kafka-storage &> /dev/null; then
    echo >&2 "The 'kafka-storage' command was not found. Please install the Kafka command line utilities."
    exit 1
  fi
}

# Start a fresh Kafka instance using KRaft. By "fresh", I mean "delete all the existing data"!
#
# In part, this function follows the steps outlined in the quick start guide https://kafka.apache.org/quickstart
startKafkaFresh() {
  # Create a clean slate for the Kafka data logs directory. This directory and subdirectories inside of it are used by
  # Kafka to store program log output, data logs, etc.
  #
  # WARNING: This is a destructive operation! This deletes everything that may already exist there from previous
  # executions of the Kafka broker. This is what we want for a local development workflow.
  mkdir -p tmp-kafka-data-logs-a/
  mkdir -p tmp-kafka-data-logs-b/
  rm -rf   tmp-kafka-data-logs-a/*
  rm -rf   tmp-kafka-data-logs-b/*

  # Generate cluster IDs
  local uuidA=$(kafka-storage random-uuid)
  local uuidB=$(kafka-storage random-uuid)

  # Format Storage Directories
  kafka-storage format -t "$uuidA" -c server-a.properties
  kafka-storage format -t "$uuidB" -c server-b.properties

  # Configure custom values
  export KAFKA_LOG4J_OPTS="-Dlog4j.configuration=file:log4j.properties"

  # Start the servers!
  echo "Starting Kafka broker 'A'..."
  # Notice the "-daemon" flag. This is useful because it means the logs won't show up in the terminal.
  kafka-server-start -daemon "server-a.properties"
  echo "Starting Kafka broker 'B'..."
  kafka-server-start -daemon "server-b.properties"
}

# Use kcat to check if Kafka is up and running. There is a timeout built in to the metadata query ('-L' command)
# of 5 seconds https://github.com/edenhill/kcat/issues/144
checkKafka() {
  kcat -L -b "$BROKER_ORIGIN"
}

waitForUp() {
  for i in $(seq 1 $ATTEMPTS) ; do
      if ((i > 1)); then
        echo "Checking if Kafka is up and running..."
      fi

      # We expect that the checkKafka function will sometimes have a non-zero exit code, so we have to set 'set +e'
      # so that the script doesn't exit.
      set +e
      if checkKafka &> /dev/null; then
        set -e
        # Change text output to bold. See https://stackoverflow.com/a/20983251
        tput bold
        echo "Kafka broker '$BROKER_NAME' is up and running at $BROKER_ORIGIN!"
        tput sgr0
        return
      fi
      set -e
  done

  # Change text output color to red. See https://stackoverflow.com/a/20983251
  tput bold
  tput setaf 1
  echo >&2 "Gave up waiting for Kafka to be up and running!"
  tput sgr0
  exit 1
}

preconditions
startKafkaFresh
BROKER_NAME=A BROKER_ORIGIN=localhost:9092 waitForUp
BROKER_NAME=B BROKER_ORIGIN=localhost:9192 waitForUp
