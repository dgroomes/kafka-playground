plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kafka.client)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
}
