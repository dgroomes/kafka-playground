// This is a "pre-compiled script plugin", or a "convention plugin". See the Gradle docs: https://docs.gradle.org/current/samples/sample_convention_plugins.html#compiling_convention_plugins

plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

