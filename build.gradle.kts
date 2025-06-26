plugins {
    // Use a stable Kotlin version so IDEs can resolve the Gradle plugin.
    // apply false so subprojects can apply it themselves
    kotlin("jvm") version "1.9.22" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        dependencies {
            testImplementation(kotlin("test"))
            testImplementation("io.kotest:kotest-assertions-core:5.8.1")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
