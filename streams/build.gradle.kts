plugins {
    java
    application
}

val slf4jVersion = "1.7.32" // SLF4J releases: http://www.slf4j.org/news.html
val kafkaVersion = "3.0.0" // Kafka releases: https://kafka.apache.org/downloads

val junitJupiterVersion = "5.8.1" // JUnit releases: https://junit.org/junit5/docs/current/release-notes/index.html
val assertJVersion = "3.21.0" // AssertJ releases: https://github.com/assertj/assertj-core/tags

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
}

tasks {
    test {
        useJUnitPlatform()

        testLogging {
            showStandardStreams = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

application {
    mainClass.set("dgroomes.kafkaplayground.streams.Main")
}
