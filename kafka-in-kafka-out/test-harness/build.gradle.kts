val junitJupiterVersion = "5.8.1" // JUnit releases: https://junit.org/junit5/docs/current/release-notes/index.html
val assertJVersion = "3.21.0" // releases: https://github.com/assertj/assertj-core/tags

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
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
}
