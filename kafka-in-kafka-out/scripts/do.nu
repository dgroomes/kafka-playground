# The commands in this script run in 'Nushell'.

# print "hi from 'do.nu'"

export def "do start-kafka" [] {
    cd $PROJECT_DIR
    ./scripts/start-kafka.sh
}

export def "do create-topics" [] {
    cd $PROJECT_DIR
    ./scripts/create-topics.sh
}

export def "do run" [mode] {
    cd $PROJECT_DIR
    ./gradlew app:installDist --quiet
    ./app/build/install/app/bin/app $mode
}

export def "do test" [] {
    cd $PROJECT_DIR
    ./gradlew test-harness:installDist --quiet
    ./test-harness/build/install/test-harness/bin/test-harness
}

export def "do stop-kafka" [] {
    cd $PROJECT_DIR
    ./scripts/stop-kafka.sh
}

export def "do topic-offsets" [] {
    cd $PROJECT_DIR
    kafka-run-class org.apache.kafka.tools.GetOffsetShell --broker-list localhost:9092
}

export def "do watch-all-consumer-groups" [] {
    while true {
        sleep 1sec
        date now | format date %T | print $in
        kafka-consumer-groups --bootstrap-server localhost:9092 --list --state
        print ""
    }
}

export def "do watch-consumer-group" [] {
    while true {
        sleep 1sec
        date now | format date %T | print --no-newline $in
        # This command shows the offsets.
        kafka-consumer-groups --bootstrap-server localhost:9092 --group my-group --describe
        print ""
        print ""
    }
}

export def "do reset-kafka" [] {
    do stop-kafka
    do start-kafka
    do create-topics
}

export def "do observe-input-topic" [] {
    # Unfortunately, even though I'm using the '-u' flag for unbuffered output, I think there's some buffering on
    # Nushell's end and two messages are always backed up inside the buffer. Only when new messages come or the command
    # is terminated do we see the buffered messages.
    kcat -CJu -o end -b localhost:9092 -t input-text | lines | each { from json | select ts headers payload }
}

export def "do observe-output-topic" [] {
    kcat -CJu -o end -b localhost:9092 -t lowest-word | lines | each { from json | select ts payload }
}

export def "do describe-topics" [] {
    kafka-topics --bootstrap-server localhost:9092 --describe
}

export def "do load" [--messages = 100 --numbers-per-message = 100 --sort-factor = 100] {
    cd $PROJECT_DIR
    ./gradlew load-simulator:installDist --quiet
    ./load-simulator/build/install/load-simulator/bin/load-simulator $messages $numbers_per_message $sort_factor
}


