val slf4jVersion = "1.7.32" // SLF4J releases: http://www.slf4j.org/news.html
val kafkaClientVersion = "3.0.0" // Kafka releases: https://kafka.apache.org/downloads

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "java")
    repositories {
        mavenCentral()
    }

    dependencies {
        "implementation"("org.slf4j:slf4j-api:$slf4jVersion")
        "implementation"("org.slf4j:slf4j-simple:$slf4jVersion")
        "implementation"("org.apache.kafka:kafka-clients:$kafkaClientVersion")
    }
}
