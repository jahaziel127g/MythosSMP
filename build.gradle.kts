plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "com.mythossmp"
version = "1.7.0-alpha"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.11.0")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.53.2.1")

    compileOnly("net.kyori:adventure-api:5.2.0")
    compileOnly("net.kyori:adventure-text-minimessage:5.2.0")

    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.2")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    create("testIntegration") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

dependencies {
    "testIntegrationImplementation"(platform("org.junit:junit-bom:6.1.2"))
    "testIntegrationImplementation"("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    "testIntegrationImplementation"("org.junit.jupiter:junit-jupiter")
    "testIntegrationImplementation"("org.mockito:mockito-core:5.23.0")
    "testIntegrationImplementation"("org.mockito.kotlin:mockito-kotlin:5.3.1")
    "testIntegrationImplementation"("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.110.0") {
        exclude("org.junit.jupiter")
    }
    "testIntegrationImplementation"("com.zaxxer:HikariCP:5.1.0")
    "testIntegrationImplementation"("org.xerial:sqlite-jdbc:3.53.2.1")
    "testIntegrationImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    "testIntegrationRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests against a bootstrapped Paper server"
    group = "verification"
    testClassesDirs = sourceSets["testIntegration"].output.classesDirs
    classpath = sourceSets["testIntegration"].runtimeClasspath
    shouldRunAfter(tasks.test)
    useJUnitPlatform()
}

val stressTest = tasks.register<Test>("stressTest") {
    description = "Runs high-load stress tests against a bootstrapped Paper server"
    group = "verification"
    testClassesDirs = sourceSets["testIntegration"].output.classesDirs
    classpath = sourceSets["testIntegration"].runtimeClasspath
    filter { includeTestsMatching("com.mythossmp.stress.*") }
    shouldRunAfter(tasks.test)
    useJUnitPlatform()
}

tasks.check {
    dependsOn(integrationTest)
}

tasks.test {
    useJUnitPlatform()
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
    runServer {
        minecraftVersion("1.21.1")
        doFirst {
            val propsFile = runDirectory.get().asFile.resolve("server.properties")
            if (!propsFile.exists()) {
                propsFile.parentFile.mkdirs()
                propsFile.writeText("online-mode=false\nspawn-protection=0\nview-distance=8\n")
            }
        }
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("MythosSMP-${version}.jar")

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

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
