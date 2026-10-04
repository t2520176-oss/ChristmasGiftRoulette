// Compile-only check of the shared Compose UI (app/.../ui/**) plus the core engine on the plain JVM,
// using JetBrains Compose from Maven Central. It exists so UI code can be type-checked where the
// Android SDK / Google Maven are unavailable. The real APK is built with the Android Gradle Plugin.
plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.3"
}

sourceSets {
    main {
        kotlin.srcDir("../../core/src/main/kotlin")
        kotlin.srcDir("../../app/src/main/java/com/lifeyourchoice/app/ui")
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.animation)
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
