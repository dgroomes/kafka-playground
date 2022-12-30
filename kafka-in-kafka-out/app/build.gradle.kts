plugins {
    application
    id("common")
}

dependencies {
    implementation(libs.slf4j.api)
    runtimeOnly(libs.slf4j.simple)
    implementation(libs.kafka.client)
}

application {
    mainClass.set("dgroomes.Main")
}
