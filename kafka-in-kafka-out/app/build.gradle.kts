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
    implementation(project(":kafka-consumer-synchronous"))
    implementation(project(":kafka-consumer-with-coroutines"))
    implementation(project(":kafka-consumer-with-virtual-threads"))

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
    mainClass.set("dgroomes.kafka_in_kafka_out.app.MainKt")
}
