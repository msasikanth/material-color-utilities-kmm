plugins {
    kotlin("multiplatform") version "1.8.10"
    id("maven-publish")
}

group = "dev.sasikanth"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    ios()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting
        val commonTest by getting
    }
}
