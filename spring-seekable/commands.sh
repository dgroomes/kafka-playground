#!/usr/bin/env bash

SEEKABLE_KAFKA_ROOT_DIR=$(pwd)
export SEEKABLE_KAFKA_ROOT_DIR

# Start Kafka
startKafka() {
  "$SEEKABLE_KAFKA_ROOT_DIR"/scripts/start-kafka.sh
}

# Stop Kafka
stopKafka() {
  "$SEEKABLE_KAFKA_ROOT_DIR"/scripts/stop-kafka.sh
}

# Build (without the tests)
build() {
  "$SEEKABLE_KAFKA_ROOT_DIR"/scripts/build.sh
}

# Run the app
run() {
  "$SEEKABLE_KAFKA_ROOT_DIR"/scripts/run.sh
}

# Execute the tests
# 'test' is a command already. Strange stuff happens if you try to declare a function called 'test'
# So let's call this 'runTests'.
runTests() {
  "$SEEKABLE_KAFKA_ROOT_DIR"/scripts/test.sh
}

# Get the current offsets of the app's Kafka consumer group
currentOffsets() {
  "$SEEKABLE_KAFKA_ROOT_DIR"/scripts/current-offsets.sh
}

# Consume from the Kafka topic
consume() {
  "$SEEKABLE_KAFKA_ROOT_DIR"/scripts/consume.sh
}

# Produce test messages to the Kafka topic
produce() {
  "$SEEKABLE_KAFKA_ROOT_DIR"/scripts/produce.sh "$@"
}
