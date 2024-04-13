plugins {
    application
    id("common")
}

dependencies {
    implementation(libs.kafka.client)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.slf4j.simple)
}

application {
    mainClass.set("dgroomes.kafka_in_kafka_out.app.Main")
}
