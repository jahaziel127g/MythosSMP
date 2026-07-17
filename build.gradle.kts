plugins {
    kotlin("jvm") version "1.9.22"
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.mythossmp"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")

    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("MythosSMP-${version}.jar")

        relocate("kotlin", "com.mythossmp.libs.kotlin")
        relocate("kotlinx", "com.mythossmp.libs.kotlinx")
        relocate("com.zaxxer.hikari", "com.mythossmp.libs.hikari")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching("**/plugin.yml") {
            expand("version" to project.version)
        }
    }
}
