#!/usr/bin/env bash
# Produce a test JSON message to the `my-messages` Kafka topic

set -eu

SECOND=$(date -j +%-S)

echo "{\"message\": \"hello\", \"time\": $SECOND}" | kcat -P -b localhost:9092 -t my-messages
