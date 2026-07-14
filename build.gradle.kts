plugins {
    kotlin("multiplatform") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
    id("com.android.application") version "8.13.2"
}

group = "org.example"

// Чтение версии из version.txt
val versionFile = file("version.txt")
val versionLines = if (versionFile.exists()) versionFile.readLines() else listOf("APP_VERSION=1.0.0", "APP_VERSION_CODE=1")
val versionMap = versionLines
    .filter { it.contains("=") }
    .associate {
        val (key, value) = it.split("=", limit = 2)
        key.trim() to value.trim()
    }
val appVersion: String = versionMap["APP_VERSION"] ?: "1.0.0"
val appVersionCode: Int = (versionMap["APP_VERSION_CODE"] ?: "1").toInt()

version = appVersion

val generateAppVersion by tasks.registering {
    val outFile = layout.projectDirectory.file("src/commonMain/kotlin/org/example/animation/AppVersion.kt")
    inputs.property("appVersion", appVersion)
    outputs.file(outFile)
    doLast {
        outFile.asFile.parentFile.mkdirs()
        outFile.asFile.writeText(
            "package org.example.animation\n\n" +
            "// Файл генерируется Gradle из version.txt. Не редактировать вручную.\n" +
            "const val APP_VERSION = \"$appVersion\"\n"
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateAppVersion)
}

kotlin {
    jvmToolchain(21)
    
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.materialIconsExtended)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation("androidx.activity:activity-compose:1.9.3")
                // FFmpeg для Android (full-gpl включает x264 для mp4)
                implementation("com.arthenica:ffmpeg-kit-full-gpl:6.0")
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                // FFmpeg для JVM (автоматически скачивает бинарники под ОС)
                implementation("ws.schild:jave-all-deps:3.3.1")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.example.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "MaryMe"
            packageVersion = appVersion
        }
    }
}

android {
    namespace = "org.example.mary_me"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.example.mary_me"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersion
        
        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86_64")
        }
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Исправляем возможные дубликаты из-за jave-all-deps в тестах или при сборке
            pickFirsts += "META-INF/kotlinx-serialization-json.kotlin_module"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}