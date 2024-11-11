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
    implementation(project(":kafka-consumer-concurrent-across-partitions-within-same-poll"))
    implementation(project(":kafka-consumer-concurrent-across-partitions"))
    implementation(project(":kafka-consumer-concurrent-across-keys"))
    implementation(project(":kafka-consumer-concurrent-across-keys-with-coroutines"))

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
    mainClass.set("dgroomes.runner.MainKt")
}
