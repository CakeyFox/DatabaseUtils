import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    kotlin("plugin.serialization") version "2.2.21"
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "net.cakeyfox.foxy"
version = "1.0.0"

kotlin {
    jvm()
    js {
        browser()
    }

    applyDefaultHierarchyTemplate()

    targets.configureEach {
        withSourcesJar()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.mongodb:bson-kotlinx:5.3.0")
                implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.3.0")
                implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.0")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")
                implementation("com.google.guava:guava:33.5.0-jre")
                implementation("ch.qos.logback:logback-classic:1.5.8")
                implementation("io.github.microutils:kotlin-logging:2.1.23")
            }
        }

        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
    }
}

mavenPublishing {
    publishToMavenCentral()
    coordinates(group.toString(), "core", version.toString())
}