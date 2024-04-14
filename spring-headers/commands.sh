#!/usr/bin/env bash

export SPRING_HEADERS_ROOT_DIR=$(pwd)

# Start Kafka
startKafka() {
  "$SPRING_HEADERS_ROOT_DIR"/scripts/start-kafka.sh $@
}

# Stop Kafka
stopKafka() {
  "$SPRING_HEADERS_ROOT_DIR"/scripts/stop-kafka.sh $@
}

# Build (without the tests)
build() {
  "$SPRING_HEADERS_ROOT_DIR"/scripts/build.sh
}

# Run the app
run() {
  "$SPRING_HEADERS_ROOT_DIR"/scripts/run.sh
}

# Produce a test message to the Kafka topic with a "type" header equal to "dgroomes.spring_headers.model.A"
produceMessageA() {
  "$SPRING_HEADERS_ROOT_DIR"/scripts/produce.sh dgroomes.spring_headers.model.MessageA
}
