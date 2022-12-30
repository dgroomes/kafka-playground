plugins {
    application
    id("common")
}

application {
    mainClass.set("dgroomes.Main")
}

dependencies {
    implementation(libs.slf4j.api)
    runtimeOnly(libs.slf4j.simple)
    implementation(libs.kafka.client)
}
