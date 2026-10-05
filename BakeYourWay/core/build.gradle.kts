import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    // The unit tests load the very same JSON files that ship inside the APK.
    systemProperty("bakeyourway.assets", rootProject.file("app/src/main").absolutePath)
    // The tests read the real recipe data and manifest, so changing them must re-run the tests.
    inputs.dir(rootProject.file("app/src/main/assets"))
    inputs.file(rootProject.file("app/src/main/AndroidManifest.xml"))
    inputs.dir(rootProject.file("app/src/main/java"))
    // Optional: lets a scratch test write a dump somewhere (used to compare outputs across refactors).
    System.getProperty("golden.out")?.let { systemProperty("golden.out", it) }
    testLogging {
        events("failed", "passed", "skipped")
        showStandardStreams = false
    }
}
