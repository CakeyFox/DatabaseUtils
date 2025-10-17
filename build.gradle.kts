plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    `maven-publish`
}

group = "net.cakeyfox.foxy"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    // MongoDB
    implementation("org.mongodb:bson-kotlinx:5.3.0")
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.3.0")

    // Date & Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.3")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:1.8.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")

    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("ch.qos.logback:logback-classic:1.5.8")
    implementation("io.github.microutils:kotlin-logging:2.1.23")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

kotlin {
    jvmToolchain(17)
}