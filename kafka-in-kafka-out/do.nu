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

def run_options [] {
    [sync async-virtual-threads async-coroutines]
}

export def "run" [mode: string@run_options] {
    cd $env.DO_DIR
    ./gradlew app:installDist --quiet
    ./app/build/install/app/bin/app $mode
}

def test_options [] {
    [one-message multi-message]
}

export def "test" [case : string@test_options] {
    cd $env.DO_DIR
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
        kafka-consumer-groups --bootstrap-server localhost:9092 --group my-group --describe
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
    # Unfortunately, even though I'm using the '-u' flag for unbuffered output, I think there's some buffering on
    # Nushell's end and two messages are always backed up inside the buffer. Only when new messages come or the command
    # is terminated do we see the buffered messages.
    kcat -CJu -o end -b localhost:9092 -t input-text | lines | each { from json | select ts headers payload }
}

export def "observe-output-topic" [] {
    kcat -CJu -o end -b localhost:9092 -t lowest-word | lines | each { from json | select ts payload }
}

export def "describe-topics" [] {
    kafka-topics --bootstrap-server localhost:9092 --describe
}

export def "load cpu-intensive" [--messages = 100 --numbers-per-message = 100 --sort-factor = 100] {
    cd $env.DO_DIR
    ./gradlew test-harness:installDist --quiet
    ./test-harness/build/install/test-harness/bin/test-harness load-cpu-intensive $messages $numbers_per_message $sort_factor
}


