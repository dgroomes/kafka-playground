#!/usr/bin/env bash

KAFKA_STREAMS_ZIP_CODES_ROOT_DIR=$(pwd)
export KAFKA_STREAMS_ZIP_CODES_ROOT_DIR

# Build (without the tests)
build() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/build.sh
}

# Run the app
run() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/run.sh
}

# Get the current offsets of the app's Kafka consumer group
currentOffsets() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/current-offsets.sh
}

# Consume from the city statistics output Kafka topic
consumeCityStats() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/consume-city-stats.sh
}

# Consume from the state statistics output Kafka topic
consumeStateStats() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/consume-state-stats.sh
}

# Consume from the overall statistics output Kafka topic
consumeOverallStats() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/consume-overall-stats.sh
}

# Consume from the input Kafka topic
consumeInput() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/consume-input.sh
}

# Produce two ZIP area records for the city Springfield, MA to the input Kafka topic.
# This function, in combination with produceSpringfield2() is useful as I test the incremental
# merge functionality.
produceSpringfield1() {
  sed -n '48,49p' zips.jsonl | kcat -P -b localhost:9092 -t streams-zip-codes-zip-areas
}

# Produce one more ZIP area record for Springfield to the input Kafka topic.
# See produceSpringfield1.
produceSpringfield2() {
  sed -n '50p' zips.jsonl | kcat -P -b localhost:9092 -t streams-zip-codes-zip-areas
}

# Produce all ZIP area records
produceAll() {
  cat zips.jsonl | kcat -P -b localhost:9092 -t streams-zip-codes-zip-areas
}

# Create the input and output Kafka topics
createTopics() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/create-topics.sh
}

# Start Kafka
startKafka() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/start-kafka.sh
}

# Stop  Kafka
stopKafka() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/stop-kafka.sh
}

# Clean the Kafka Streams state directory (RocksDB data) for when things get messed up
cleanState() {
  rm -rf /tmp/kafka-streams/streams-zip-codes
}

# A compound command to reset the Kafka broker and state
reset() {
 stopKafka && cleanState && startKafka && createTopics
}
