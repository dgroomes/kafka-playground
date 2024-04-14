plugins {
    java
    application
}

val springBootVersion = "3.2.4" // Spring Boot releases: https://spring.io/projects/spring-boot#learn
val slf4jVersion = "2.0.12" // SLF4J releases: http://www.slf4j.org/news.html

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))

    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.kafka:spring-kafka")

    // Add Jackson as a runtime dependency because it will enable the Spring Kafka "type-detection machinery" that
    // we are trying to explore in this subproject.
    runtimeOnly("com.fasterxml.jackson.core:jackson-databind")
}

application {
    mainClass.set("dgroomes.spring_headers.Main")
}
