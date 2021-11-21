#!/usr/bin/env bash
# Produce test messages to the Kafka topic
# For example, the following command will produce ten messages: ./produce.sh 10

REPETITIONS=${1:-1}

SECONDS=$(date +%s)

for i in $(seq 1 $REPETITIONS); do
  echo "hello! Iteration=$i Time=$SECONDS" | kcat -P -b localhost:9092 -t plaintext-input
done
