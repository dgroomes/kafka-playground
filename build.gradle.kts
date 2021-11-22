/**
 * A convenience task to assemble all of the included projects. This is useful as a quick smoke test.
 */
tasks.register("assembleAll") {
    dependsOn(gradle.includedBuild("interactive").task(":assemble"))
    dependsOn(gradle.includedBuild("connection-check").task(":assemble"))
    dependsOn(gradle.includedBuild("kafka-in-kafka-out").task(":app:assemble"))
    dependsOn(gradle.includedBuild("kafka-in-kafka-out").task(":load-simulator:assemble"))
    dependsOn(gradle.includedBuild("kafka-in-kafka-out").task(":test-harness:assemble"))
    dependsOn(gradle.includedBuild("spring-barebones").task(":assemble"))
    dependsOn(gradle.includedBuild("spring-errors").task(":assemble"))
    dependsOn(gradle.includedBuild("spring-headers").task(":assemble"))
    dependsOn(gradle.includedBuild("spring-seekable").task(":assemble"))
    dependsOn(gradle.includedBuild("spring-headers").task(":assemble"))
    dependsOn(gradle.includedBuild("streams").task(":assemble"))
}
