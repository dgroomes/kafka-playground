#!/usr/bin/env bash
# Produce a test message to the Kafka topic with a "type" header

if [[ -z "$1" ]]; then
  echo >&2 "missing argument. Must provide a 'type' for the header. For example: $0 dgroomes.kafkaplayground.springheaders.model.MessageA"
  exit 1
fi

TYPE=$1

set -eu

SECOND=$(date -j +%S)

echo "{\"message\": \"hello($SECOND)\", \"type\": \"$TYPE\", \"a\": \"A for Apple\"}" | kcat -P -b localhost:9092 -t my-messages -H "__TypeId__=$TYPE"
