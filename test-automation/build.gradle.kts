plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":game-server"))

    implementation(libs.gson)
    implementation(libs.toml4j)
    implementation(libs.slf4j.api)
    implementation(libs.log4j.api)
    runtimeOnly(libs.logback)
    runtimeOnly(libs.log4j.to.slf4j)

    // Netty for E2E game client (network-level test automation)
    implementation(libs.netty.all)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    // Config.java static initializer reads ./settings.toml from working directory
    workingDir = project(":game-server").projectDir
    // Forward -De2e=true system property to test JVM
    if (project.hasProperty("e2e") || System.getProperty("e2e") == "true") {
        systemProperty("e2e", "true")
    }
}
