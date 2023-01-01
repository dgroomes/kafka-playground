plugins {
    java
    application
    id("org.springframework.boot") version "3.0.1" // Spring Boot releases: https://spring.io/projects/spring-boot#learn
}

apply(plugin = "io.spring.dependency-management")

val slf4jVersion = "2.0.6" // SLF4J releases: http://www.slf4j.org/news.html

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
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.kafka:spring-kafka")
}
