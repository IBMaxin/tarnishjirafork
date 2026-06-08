plugins {
    kotlin("jvm")
    application
    id("org.zeroturnaround.gradle.jrebel")
    id("com.gradleup.shadow")
}

application {
    mainClass.set("com.osroyale.Main")
    applicationDefaultJvmArgs += arrayOf(
        "-XX:-OmitStackTraceInFastThrow",
        "--enable-preview",
        "-XX:+UseZGC",
        "-Xmx8g",
        "-Xms4g",
        "-XX:MaxGCPauseMillis=100"
    )
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(libs.slf4j.api)
    runtimeOnly(libs.slf4j.simple)

    implementation(libs.netty.all)
    implementation(libs.joda.time)
    implementation(libs.quartz)
    implementation(libs.jcabi.jdbc)
    implementation(libs.gson)
    implementation(libs.guava)
    implementation(libs.jsoup)
    implementation(libs.commons.compress)
    implementation(libs.toml4j)
    implementation(libs.mysql.connector)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.log4j.core)
    implementation(libs.ant)
    implementation(libs.jctools)
    implementation(libs.classgraph)
    implementation(libs.discord4j.core)
    implementation(libs.fastutil)
    implementation(libs.jda)
    implementation(libs.aho.corasick)
    implementation(libs.jbcrypt)
    implementation(libs.argon2)
    implementation(libs.openjdk.affinity)
    implementation(libs.chronicle.threads)

    testImplementation(libs.junit4)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.vintage)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

sourceSets.named("main") {
    java {
        srcDir("plugins")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.apply {
        encoding = "UTF-8"
        compilerArgs.add("--enable-preview")
    }
}

tasks.register<Exec>("refreshCodeIndex") {
    description = "Regenerate code_index.json for AI-assisted development"
    workingDir = rootProject.projectDir
    commandLine("bash", "generate_index.sh")
}
