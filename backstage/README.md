# backstage

The *backstage* is the behind-the-scenes production of the feature content in this repository.


## Kafka Bash Scripts

Starting a Kafka cluster locally for development purposes takes a few steps:

1. Delete any pre-existing data files
2. Start Kafka  
3. Wait for Kafka to be up

For the sake of a "fast, pleasant, and please-uplift-my-spirits" local development workflow it is worth automating
these steps into one step. The resulting script (`start-kafka.sh`) is ergonomic, adds some useful logging, and is
adorned with comments for the curious. There is also a `stop-kafka.sh` script.

These scripts should be copied and pasted into a project where you need to run a Kafka cluster locally. Tweak the scripts
to your needs!

Copy the scripts to the other subprojects with:

```nushell
./copy-scripts.sh
```


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [x] DONE Add a wait loop in the stop script. I'm pretty sure Kafka takes a while to stop sometimes and if you kick off the stop
  command without waiting for Kafka to come to a complete stop, then it's undefined what happens next if you execute other
  commands in the meantime.
* [x] DONE Replace all 'kafkacat' references with the new 'kcat' name since that project had to change names.
* [x] DONE (That was a little harder than I thought because of the confusion of Kafka's `log.dir` vs `log.dirs` and
  `LOG_DIR` configs and the separation of the KRaft `kafka-storage format` step from starting the server ) In recent
  versions of Kafka, KRaft-mode is production ready. Use the bundled `kraft/server.properties` file in the Kafka
  distribution as a basis for the `server.properties` file copied to the subprojects.
* [x] DONE Consider bundling a `server.properties`. I want to embed some commentary in it.
  * I definitely see that pointing a first-time Kafka operator to the bundled `kraft/server.properties` is a good idea.
    But I want to de-noise this config file and add what I think are essential commentary like a link to the official
    Kafka documentation for the config. I want to especially highlight the "important" (Kafka's own words) configs.


## Reference

* [Apache Blog: *What’s New in Apache Kafka 2.8.0*](https://blogs.apache.org/kafka/entry/what-s-new-in-apache5)
  * This release includes KIP-500 which brings experimental support for running Kafka without Zookeeper. I want to use
    this for local development, so I can drop the Zookeeper configuration and the related scripting to handle starting and
    stopping it. This mode is called "KRaft mode". Read more about it in the [KRaft README](https://github.com/apache/kafka/blob/2.8/config/kraft/README.md).

