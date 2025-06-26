plugins {
    kotlin("jvm") version "2.0.21"
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
