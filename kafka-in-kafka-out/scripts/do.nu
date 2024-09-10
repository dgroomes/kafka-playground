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

export def "do build" [] {
    cd $PROJECT_DIR
    ./gradlew app:installDist
}

export def "do run" [] {
    cd $PROJECT_DIR
    ./app/build/install/app/bin/app
}

export def "do test" [] {
    cd $PROJECT_DIR
    ./gradlew test-harness:test
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
    }
}

export def "do watch-consumer-group" [] {
    while true {
        sleep 1sec
        date now | format date %T | print $in
        # This command shows the offsets.
        kafka-consumer-groups --bootstrap-server localhost:9092 --group my-group --describe
    }
}

export def "do reset-kafka" [] {
    do stop-kafka
    do start-kafka
    do create-topics
}

export def "do observe-topics" [] {
    cd $PROJECT_DIR
    ./scripts/observe-topics.sh
}

export def "do describe-topics" [] {
    kafka-topics --bootstrap-server localhost:9092 --describe
}


