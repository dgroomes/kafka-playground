#!/usr/bin/env bash
# Produce test messages to the Kafka topic
# Example to produce ten messages: ./produce.sh 10

REPETITIONS=${1:-1}

SECONDS=$(date +%s)

for i in $(seq 1 $REPETITIONS); do
  echo "hello($SECONDS) $i" | kcat -P -b localhost:9092 -t my-messages
done
