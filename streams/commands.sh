#!/usr/bin/env bash

KAFKA_STREAMS_ROOT_DIR=$(pwd)
export KAFKA_STREAMS_ROOT_DIR

# Build (without the tests)
build() {
  "$KAFKA_STREAMS_ROOT_DIR"/scripts/build.sh
}

# Run the app
run() {
  "$KAFKA_STREAMS_ROOT_DIR"/scripts/run.sh
}

# Get the current offsets of the app's Kafka consumer group
currentOffsets() {
  "$KAFKA_STREAMS_ROOT_DIR"/scripts/current-offsets.sh
}

# Consume from the Kafka topic
consume() {
  "$KAFKA_STREAMS_ROOT_DIR"/scripts/consume.sh
}

# Produce a test message to the Kafka topic
produce() {
  "$KAFKA_STREAMS_ROOT_DIR"/scripts/produce.sh "$@"
}

# Create the input and output Kafka topics
createTopics() {
  "$KAFKA_STREAMS_ROOT_DIR"/scripts/create-topics.sh
}

# Start Kafka
startKafka() {
  "$KAFKA_STREAMS_ROOT_DIR"/scripts/start-kafka.sh
}

# Stop  Kafka
stopKafka() {
  "$KAFKA_STREAMS_ROOT_DIR"/scripts/stop-kafka.sh
}

# Clean the Kafka Streams state directory (RocksDB data) for when things get messed up
cleanState() {
  rm -rf /tmp/kafka-streams/streams-wordcount
}

# A compound command to reset the Kafka broker and state
reset() {
 stopKafka && cleanState && startKafka && createTopics
}
