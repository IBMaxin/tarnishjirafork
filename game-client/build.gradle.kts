import com.github.jengelman.gradle.plugins.shadow.ShadowApplicationPlugin.Companion.SHADOW_INSTALL_TASK_NAME
import com.github.jengelman.gradle.plugins.shadow.ShadowApplicationPlugin.Companion.SHADOW_SCRIPTS_TASK_NAME

plugins {
    java
    application
    id("com.gradleup.shadow")
}

group = "com.osroyale"
version = "1.0"

dependencies {
    annotationProcessor(libs.lombok)

    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    compileOnly(libs.jsr305)
    compileOnly(libs.lombok)
    compileOnly(libs.orange.extensions)

    implementation(libs.logback)
    implementation(libs.gson)
    implementation(libs.guava) {
        exclude(group = "com.google.code.findbugs", module = "jsr305")
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "com.google.j2objc", module = "j2objc-annotations")
        exclude(group = "org.codehaus.mojo", module = "animal-sniffer-annotations")
    }
    implementation(libs.guice)
    implementation(libs.rxrelay)
    implementation(libs.okhttp)
    implementation(libs.rxjava)
    implementation(libs.jna)
    implementation(libs.jna.platform)
    implementation(libs.runelite.discord)
    implementation(libs.substance)
    implementation(libs.jopt.simple)
    implementation(libs.desktopsupport)
    implementation(libs.commons.text)
    implementation(libs.commons.csv)
    implementation(libs.commons.io)
    implementation(libs.jetbrains.annotations)
    implementation(libs.java.semver)
    implementation(libs.slf4j.api)

    implementation(libs.jogl.rl)
    implementation(libs.jogl.gldesktop.dbg)
    implementation(libs.jocl)

    runtimeOnly(libs.trident)

    testAnnotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)

    testImplementation(libs.guice.grapher)
    testImplementation(libs.guice.testlib)
    testImplementation(libs.junit4)
    testImplementation(libs.mockito.core)
    testImplementation(libs.okhttp)
    testImplementation(libs.slf4j.api)

    implementation(platform("org.lwjgl:lwjgl-bom:${libs.versions.lwjgl.get()}"))
    implementation(libs.lwjgl)
    implementation(libs.lwjgl.opengl)
    implementation(libs.lwjgl.opencl)
    implementation(libs.rlawt)

    listOf("linux", "macos", "macos-arm64", "windows-x86", "windows").forEach {
        runtimeOnly("org.lwjgl:lwjgl::natives-$it")
        runtimeOnly("org.lwjgl:lwjgl-opengl::natives-$it")
    }

    implementation(files("libs/SwiftFUP-client-3.7.1.jar"))
    implementation(files("libs/allatori-annotations.jar"))
    implementation(libs.fastutil)
    implementation(libs.rs.cache)

    implementation(libs.netty.handler)
    implementation(libs.netty.io.uring)
    implementation(libs.netty.epoll)
    implementation(libs.netty.kqueue)

    // Dropwizard Metrics for JGroups monitoring (kept even though JGroups is removed, as it may be used by the event bus)
}

application {
    mainClass.set("com.osroyale.Client")
    applicationDefaultJvmArgs += arrayOf(
        "-XX:-OmitStackTraceInFastThrow",
        "-Xmx2g",
        "-Xms1g"
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.apply {
            isWarnings = false
            isDeprecation = false
            encoding = "UTF-8"
        }
    }

    shadowJar {
        archiveBaseName.set("Tarnish")
        archiveClassifier.set("")
        archiveVersion.set("")
        isZip64 = true
    }

    named<Zip>("distZip").configure {
        enabled = false
    }
    named<Tar>("distTar").configure {
        enabled = false
    }
    named<CreateStartScripts>("startScripts").configure {
        enabled = false
    }
    named<CreateStartScripts>(SHADOW_SCRIPTS_TASK_NAME).configure {
        enabled = false
    }
    named(SHADOW_INSTALL_TASK_NAME).configure {
        enabled = false
    }
    named("shadowDistTar").configure {
        enabled = false
    }
    named("shadowDistZip").configure {
        enabled = false
    }
}
