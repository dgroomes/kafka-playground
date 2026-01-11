#!/usr/bin/env bash
# Stop any running Kafka brokers on the system.
#
# This is adapted from the upstream script in the Apache Kafka source code: https://github.com/apache/kafka/blob/6c9e27c2b51b3653f8a4009ca9712b3bf85ebacd/bin/kafka-server-stop.sh
# The upstream script has special handling for IBM "OS/390" and "OS/400" operating systems. I've chosen to omit that handling
# code here.
#
# This script will exit with a success code when there are no Kafka brokers to stop instead of an error code like the
# upstream script does.
#
# This script will wait for the Kafka broker process to stop before exiting instead of exiting immediately like the
# upstream script does.

PIDS=$(ps ax | grep ' kafka\.Kafka ' | grep java | grep -v grep | awk '{print $1}')

if [ -z "$PIDS" ]; then
  tput bold
  echo "No Kafka server to stop"
  tput sgr0
else
  echo "Stopping the Kafka server..."
  while kill $PIDS &> /dev/null; do
    sleep 1
  done
  tput bold
  echo "Kafka is stopped"
  tput sgr0
fi
