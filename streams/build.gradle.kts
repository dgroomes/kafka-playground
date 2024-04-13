plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kafka.client)
    implementation(libs.kafka.streams)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.slf4j.simple)

    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.kafka.streams.test.utils)

    testRuntimeOnly(libs.junit.jupiter.engine)
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
    mainClass.set("dgroomes.kafka_playground.streams.Main")
}
