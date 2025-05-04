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