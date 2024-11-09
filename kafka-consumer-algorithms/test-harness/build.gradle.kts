plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kafka.client)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.slf4j.simple)
}

application {
    mainClass.set("dgroomes.test_harness.TestHarness")
}
