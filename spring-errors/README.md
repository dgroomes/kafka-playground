# spring-errors

A basic Spring Kafka application that showcases the Spring framework features and behavior 
around Kafka error handling.

---

Spring Kafka (<https://spring.io/projects/spring-kafka>) has a significant amount of software machinery around error 
handling ([relevant section in docs](https://docs.spring.io/spring-kafka/reference/html/#annotation-error-handling)).
This project aims to de-mystify and illuminate it. Let's learn something!

### Instructions

1. Use Java 17
2. Install Kafka and `kcat`:
   * `brew install kafka`
   * `brew install kcat`
3. Running the application and the test cases depend on a locally running Kafka instance. Use the `startKafka` and 
  `stopKafka` commands (see [`commands.sh`](#commandssh)) to run Kafka.
4. In a new terminal, build and run the program with `build && run`
5. In a new terminal, produce some test data with `produceValidMessage`. You should see the application react with new 
  logs.
6. In a new terminal, produce some test data with `produceInvalidMessage`. You should see the application react with
  new logs.
7. Consume the dead letter topic with `consumeDeadLetterTopic` and you should all the "invalid" messages there. As new 
  "invalid" messages are received by the app, they will be forward to this topic.

### `commands.sh`

Source the `commands.sh` file using `source commands.sh` which will load your shell with useful 
commands. Commands include:

  * `startKafka` start Kafka
  * `stopKafka` stop Kafka
  * `build` build (without tests)
  * `run` run the app
  * `produceValidMessage` produce a test JSON message to the `my-messages` Kafka topic 
  * `produceInvalidMessage` produce a test JSON message to the `my-messages` Kafka topic that has a field of the wrong type 
    JSON that the app is expecting 
  * `consumeDeadLetterTopic` consume from the Kafka topic

### Commentary

Warning: here is some editorialization! Is Spring Kafka fundamentally oriented to "one-message-at-a-time" processing?
The framework has a great configuration story for apps with a single consumer (there is exactly one 
`spring.kafka.consmer` config block), for a single cluster, and for single (non-batch) message processing flows. There 
is a sophisticated retry/recovery feature set. But it is restricted to the non-batch style. See this comment:

> A retry adapter is not provided for any of the batch message listeners, because the framework has no knowledge of
> where in a batch the failure occurred. If you need retry capabilities when you use a batch listener, we recommend that
> you use a RetryTemplate within the listener itself.
> -- <cite>https://docs.spring.io/spring-kafka/docs/2.5.1.RELEASE/reference/html/#retrying-deliveries</cite>  
