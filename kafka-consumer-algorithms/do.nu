# This file is for my own development workflow. It is not intended to be part of the demonstration content of the rest
# of the repository.

export def "start-kafka" [] {
    cd $env.DO_DIR
    ./scripts/start-kafka.sh
}

export def "create-topics" [] {
    cd $env.DO_DIR
    ./scripts/create-topics.sh
}

def compute_options [] {
    [in-process-compute remote-compute]
}

def consumer_options [] {
    [sequential parallel-within-same-poll-consumer concurrent-across-partitions concurrent-across-keys concurrent-across-keys-with-coroutines]
}

export def "run" [compute: string@compute_options consumer: string@consumer_options] {
    cd $env.DO_DIR
    ./gradlew example-consumer-app:installDist --quiet
    ./example-consumer-app/build/install/example-consumer-app/bin/example-consumer-app $"($compute):($consumer)"
}

def test_options [] {
    [one-message multi-message]
}

export def "test" [case : string@test_options] {
    cd $env.DO_DIR
    ./gradlew test-harness:installDist --quiet
    ./test-harness/build/install/test-harness/bin/test-harness $case
}

export def "stop-kafka" [] {
    cd $env.DO_DIR
    ./scripts/stop-kafka.sh
}

export def "topic-offsets" [] {
    cd $env.DO_DIR
    kafka-run-class org.apache.kafka.tools.GetOffsetShell --broker-list localhost:9092
}

export def "watch-all-consumer-groups" [] {
    while true {
        sleep 1sec
        date now | format date %T | print $in
        kafka-consumer-groups --bootstrap-server localhost:9092 --list --state
        print ""
    }
}

export def "watch-consumer-group" [] {
    while true {
        sleep 1sec
        date now | format date %T | print --no-newline $in
        # This command shows the offsets.
        kafka-consumer-groups --bootstrap-server localhost:9092 --group app --describe
        print ""
        print ""
    }
}

export def "reset-kafka" [] {
    stop-kafka
    start-kafka
    create-topics
}

export def "observe-input-topic" [] {
    # Unfortunately, I'm having trouble using Nushell here because I think there's some extra buffering. Instead, let's
    # shell out to Bash..
    bash -c 'kcat -CJu -o end -b localhost:9092 -t input | jq --unbuffered'
}

export def "observe-output-topic" [] {
    bash -c 'kcat -CJu -o end -b localhost:9092 -t output | jq --unbuffered'
}

export def "describe-topics" [] {
    kafka-topics --bootstrap-server localhost:9092 --describe
}

export def load [] {
    cd $env.DO_DIR
    ./gradlew test-harness:installDist --quiet
    ./test-harness/build/install/test-harness/bin/test-harness load
}

export def "load-uneven" [] {
        cd $env.DO_DIR
        ./gradlew test-harness:installDist --quiet
        ./test-harness/build/install/test-harness/bin/test-harness load-uneven
}
