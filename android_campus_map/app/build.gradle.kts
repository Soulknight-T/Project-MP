@file:Suppress("SpellCheckingInspection", "MagicNumber", "GradleDependency")

import java.util.function.BiConsumer
import java.util.UUID

/*
 * This file configures the build system that creates your Android app.
 * The syntax is Kotlin, not Java.
 * You do not need to understand the contents of this file, nor should you modify it.
 * Any changes will be overwritten during official grading.
 */

buildscript {
    repositories {
        mavenCentral()
        google()
    }
}
plugins {
    id("com.android.application")
    id("com.github.cs125-illinois.gradlegrader") version "2022.10.0"
    checkstyle
    id("com.github.sherter.google-java-format") version "0.9"
}
android {
    compileSdk = 33
    defaultConfig {
        applicationId = "edu.illinois.cs.cs124.ay2022.favoriteplaces"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    namespace = "edu.illinois.cs.cs124.ay2022.mp"
}

dependencies {
    /*
     * Do not add dependencies here, since they will be overwritten during official grading.
     * If you have a package that you think would be broadly useful for completing the MP, please start a discussion
     * on the forum.
     */

    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.android.volley:volley:1.2.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.2")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:mockwebserver:4.10.0")
    implementation("org.osmdroid:osmdroid-android:6.1.14")
    implementation("com.opencsv:opencsv:5.7.0")

    testImplementation("com.github.cs125-illinois:gradlegrader:2022.10.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.9")
    testImplementation("androidx.test:core:1.4.0")
    testImplementation("androidx.test.ext:junit:1.1.3")
    testImplementation("androidx.test.ext:truth:1.4.0")
    testImplementation("androidx.test.espresso:espresso-core:3.4.0")
    testImplementation("androidx.test.espresso:espresso-intents:3.4.0")
    testImplementation("androidx.test.espresso:espresso-contrib:3.4.0")
}
googleJavaFormat {
    toolVersion = "1.7"
}
checkstyle {
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    toolVersion = "9.0"
}
tasks.register("checkstyle", Checkstyle::class) {
    source("src/main/java")
    include("**/*.java")
    classpath = files()
}
gradlegrader {
    assignment = "AY2022.MP"
    checkpoint {
        yamlFile = rootProject.file("grade.yaml")
        configureTests(
                BiConsumer { MP, test ->
                    require(MP in setOf("0", "1", "2", "3")) { "Cannot grade unknown checkpoint MP$MP" }
                    test.setTestNameIncludePatterns(listOf("MP${MP}Test"))
                    test.filter.isFailOnNoMatchingTests = true
                }
        )
    }
    checkstyle {
        points = 10
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        version = "9.0"
    }
    forceClean = false
    identification {
        txtFile = rootProject.file("ID.txt")
        validate = Spec {
            try {
                UUID.fromString(it.trim())
                true
            } catch (e: java.lang.IllegalArgumentException) {
                false
            }
        }
    }
    reporting {
        post {
            endpoint = "https://cloud.cs124.org/gradlegrader"
        }
        printPretty {
            title = "Grade Summary"
            notes = "On checkpoints with an early deadline, the maximum local score is 90/100. " +
                    "10 points will be provided during official grading if you submit code " +
                    "that meets the early deadline threshold before the early deadline."
        }
    }
    vcs {
        git = true
        requireCommit = true
    }
}
