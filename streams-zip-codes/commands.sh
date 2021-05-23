#!/usr/bin/env bash

export KAFKA_STREAMS_ZIP_CODES_ROOT_DIR=$(pwd)

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

# Consume from the output Kafka topic
consume() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/consume.sh
}

# Consume from the input Kafka topic
consumeInput() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/consume-input.sh
}

# Produce two ZIP area records for the city Springfield, MA to the input Kafka topic.
# This function, in combination with produceSpringfield2() is useful as I test the incremental
# merge functionality.
produceSpringfield1() {
  sed -n '48,49p' zips.json | kafkacat -P -b localhost:9092 -t streams-zip-codes-zip-areas
}

# Produce one more ZIP area record for Springfield to the input Kafka topic.
# See produceSpringfield1.
produceSpringfield2() {
  sed -n '50p' zips.json | kafkacat -P -b localhost:9092 -t streams-zip-codes-zip-areas
}

# Produce all ZIP area records
produceAll() {
  cat zips.json | kafkacat -P -b localhost:9092 -t streams-zip-codes-zip-areas
}

# Create the input and output Kafka topics
createTopics() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/create-topics.sh $@
}

# Start Kafka
startKafka() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/start-kafka.sh $@
}

# Stop  Kafka
stopKafka() {
  "$KAFKA_STREAMS_ZIP_CODES_ROOT_DIR"/scripts/stop-kafka.sh $@
}

# A compound command to reset the Kafka broker and state
reset() {
 stopKafka && startKafka && createTopics
}
