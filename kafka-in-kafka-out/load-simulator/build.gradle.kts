plugins {
    application
    id("common")
}

application {
    mainClass.set("dgroomes.kafka_in_kafka_out.load_simulator.Main")
}

dependencies {
    implementation(libs.kafka.client)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.slf4j.simple)
}
