#!/usr/bin/env bash

export SPRING_MULTI_BROKER_ROOT_DIR=$(pwd)

# Start Kafka and Zookeeper
startKafka() {
  "$SPRING_MULTI_BROKER_ROOT_DIR"/scripts/start-kafka.sh $@
}

# Stop Kafka and Zookeeper
stopKafka() {
  "$SPRING_MULTI_BROKER_ROOT_DIR"/scripts/stop-kafka.sh $@
}

# Build (without the tests)
build() {
  "$SPRING_MULTI_BROKER_ROOT_DIR"/scripts/build.sh
}

# Run the app
run() {
  "$SPRING_MULTI_BROKER_ROOT_DIR"/scripts/run.sh
}

# Produce a test message to the "A" Kafka broker
produceBrokerA() {
  echo "hello A!" | kcat -P -b localhost:9092 -t my-messages
}

# Produce a test message to the "B" Kafka broker
produceBrokerB() {
  echo "hello B!" | kcat -P -b localhost:9192 -t my-messages
}
