plugins {
    id("common")
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kafka.client)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
}
