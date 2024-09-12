plugins {
    application
    id("common")
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kafka.client)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
    implementation(project(":kafka-high-level-consumer"))

    runtimeOnly(libs.slf4j.simple)
}

application {
    mainClass.set("dgroomes.kafka_in_kafka_out.app.Main")
}
