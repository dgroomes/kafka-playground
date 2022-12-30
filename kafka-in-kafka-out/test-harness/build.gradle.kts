plugins {
    java
    id("common")
}

tasks {
    test {
        useJUnitPlatform()

        /**
         * Force the tests to always run because the application-under-test is out-of-process. In fact, the
         * application-under-test might as well be a GoLang or C project. The test code is a standalone test harness
         * to send and receive messages to and from Kafka. The test harness has no idea if the application-under-test
         * has changed or not, so we will opt out of Gradle's task avoidance feature with the `outputs.upToDateWhen { false }`
         * trick. This forces the 'test' task to always run.
         */
        outputs.upToDateWhen { false }

        testLogging {
            showStandardStreams = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

dependencies {
    testImplementation(libs.slf4j.api)
    testRuntimeOnly(libs.slf4j.simple)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(libs.kafka.client)
    testImplementation(libs.assertj)
}
