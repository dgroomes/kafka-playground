plugins {
    java
    application
}

val slf4jVersion = "1.7.32" // SLF4J releases: http://www.slf4j.org/news.html
val kafkaVersion = "3.0.0" // Kafka releases: https://kafka.apache.org/downloads
val jacksonVersion = "2.13.0" // Jackson releases: https://github.com/FasterXML/jackson/wiki/Jackson-Releases

val junitJupiterVersion = "5.8.1" // JUnit releases: https://junit.org/junit5/docs/current/release-notes/index.html
val assertJVersion = "3.21.0" // releases: https://github.com/assertj/assertj-core/tags

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
    implementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))

    // jackson-module-parameter names is needed to support deserializing to Java record classes
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names")

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
    mainClass.set("dgroomes.kafkaplayground.streamszipcodes.Main")
}
