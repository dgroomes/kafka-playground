plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
    implementation(libs.kafka.client)
}

application {
    mainClass.set("dgroomes.interactive.Main")
}
