#!/usr/bin/env bash
# Create the Kafka topic with N number of partitions.
#
# Remember, if you do not create the topic explicitly and then the topic will be created with default settings
# automatically when the app starts. Default settings specifies one partition.

if [[ "x$1" == "x" ]]; then
  echo >&2 "Usage: $0 <number of partitions>"
  exit 1
fi

kafka-topics --bootstrap-server localhost:9092 --replication-factor 1 --create --topic my-messages --partitions $1
