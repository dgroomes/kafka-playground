#!/usr/bin/env bash

export INTERACTIVE_ROOT_DIR=$(pwd)

# Start Kafka
startKafka() {
  "$INTERACTIVE_ROOT_DIR"/scripts/start-kafka.sh $@
}

# Stop Kafka
stopKafka() {
  "$INTERACTIVE_ROOT_DIR"/scripts/stop-kafka.sh $@
}

# Build
build() {
  "$INTERACTIVE_ROOT_DIR"/scripts/build.sh
}

# Run the app
run() {
  "$INTERACTIVE_ROOT_DIR"/scripts/run.sh
}

# Get the current offsets of the app's Kafka consumer group
currentOffsets() {
  "$INTERACTIVE_ROOT_DIR"/scripts/current-offsets.sh
}

# Consume from the Kafka topic
consume() {
  "$INTERACTIVE_ROOT_DIR"/scripts/consume.sh
}

# Produce a test message to the Kafka topic
produce() {
  "$INTERACTIVE_ROOT_DIR"/scripts/produce.sh $@
}

# Create the Kafka topic with N number of partitions. You should experiment with varying numbers of partitions to see
# how the app behaves. Especially pay attention to the different between one partition and two partitions (more than two
# won't be very interesting).
createTopic() {
  "$INTERACTIVE_ROOT_DIR"/scripts/create-topic.sh $@
}
