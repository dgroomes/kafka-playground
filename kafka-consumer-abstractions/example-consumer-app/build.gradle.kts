plugins {
    application
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kafka.client)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
    implementation(project(":kafka-consumer-sequential"))
    implementation(project(":kafka-consumer-parallel-within-same-poll"))
    implementation(project(":kafka-consumer-async"))
    implementation(project(":kafka-consumer-async-by-key-with-virtual-threads"))
    implementation(project(":kafka-consumer-async-by-key-with-coroutines"))

    runtimeOnly(libs.slf4j.simple)
}

tasks {
    withType<JavaExec> {
        systemProperty("kotlinx.coroutines.debug", "")
    }

    named<CreateStartScripts>("startScripts") {
        defaultJvmOpts = listOf("-Dkotlinx.coroutines.debug")
    }
}

application {
    mainClass.set("dgroomes.example_consumer_app.MainKt")
}
