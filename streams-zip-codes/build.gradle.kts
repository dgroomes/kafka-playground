plugins {
    java
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.slf4j.api)
    runtimeOnly(libs.slf4j.simple)
    implementation(libs.kafka.client)
    implementation(libs.kafka.streams)
    implementation(platform(libs.jackson.bom))

    // jackson-module-parameter names is needed to support deserializing to Java record classes
    implementation(libs.jackson.module.parameter.names)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj)
    testImplementation(libs.kafka.streams.test.utils)
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
