plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false

}

buildscript {
    repositories {

        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

        dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("com.google.gms:google-services:4.4.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    }
}

allprojects {}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}