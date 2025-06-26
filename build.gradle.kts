plugins {
    // Use a stable Kotlin version so IDEs can resolve the Gradle plugin
    kotlin("jvm") version "1.9.22"
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    dependencies {
        testImplementation(kotlin("test"))
        testImplementation("io.kotest:kotest-assertions-core:5.8.1")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
