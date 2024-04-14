plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.spring.kafka)

    runtimeOnly(libs.slf4j.simple)

    testImplementation(libs.spring.kafka.test)
}

application {
    mainClass.set("dgroomes.spring_barebones.Main")
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
