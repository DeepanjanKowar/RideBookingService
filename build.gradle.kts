plugins {
    // Use a stable Kotlin version so IDEs can resolve the Gradle plugin
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    // Add test dependencies only after the Kotlin plugin is applied so that
    // the 'testImplementation' configuration exists for each subproject.
    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            testImplementation(kotlin("test"))
            testImplementation("io.kotest:kotest-assertions-core:5.8.1")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
