#!/usr/bin/env bash
# Use kcat to consume from the input and output Kafka topics as a way to observe the data flow through the Kafka
# broker. Also, it creates the topics if they don't exist already.
#
# I need to use this as a sanity check while developing the test harness.

__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

kafka-topics --create --bootstrap-server localhost:9092 --if-not-exists --topic input-text
kafka-topics --create --bootstrap-server localhost:9092 --if-not-exists --topic quoted-text

# Kick off two kcat consumers in the background (using "&") to consume from each of the topics.
#
# Note: the "-u" flag means unbuffered and it's important to use when piping to another command or else messages will stay
# buffered in kcat and you will think that the messages aren't there! See https://github.com/edenhill/kcat/issues/160
#
# Unfortunately, kcat's support for reading from multiple topics requires a consumer group (see https://github.com/edenhill/kcat/issues/179)
# and for some reason this still buffers the output even when using the "-u" option. So, instead I need to use two independent
# kcat processes to consume from the topics.
#
# Pipe the output to a perl script that will prefix each message with a timestamp.
kcat -Cu -o end -b localhost:9092 -t input-text | perl -nf "$__dir/time-with-millis.pl" &
kcat -Cu -o end -b localhost:9092 -t quoted-text | perl -nf "$__dir/time-with-millis.pl" &

# Wait until the jobs are cancelled.
wait $(jobs -p)
