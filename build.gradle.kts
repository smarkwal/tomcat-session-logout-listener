import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java")
    jacoco
}

group = "net.markwalder"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.apache.tomcat:tomcat-catalina:9.0.65") // TODO: select correct configuration

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("org.mockito:mockito-core:4.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.7.0")
    testImplementation("org.assertj:assertj-core:3.23.1")
}

java {

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    // automatically package source code as artifact -sources.jar
    withSourcesJar()

    // automatically package Javadoc as artifact -javadoc.jar
    withJavadocJar()
}

jacoco {
    toolVersion = "0.8.8"
}

tasks.withType<JavaCompile> {
    options.encoding = "ASCII"
}

tasks.getByName<Test>("test") {

    // use JUnit 5
    useJUnitPlatform()

    // settings
    maxHeapSize = "1G"

    // test task output
    testLogging {
        events = mutableSetOf(
            TestLogEvent.STARTED,
            TestLogEvent.PASSED,
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR
        )
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

// disable generation of Gradle module metadata file
tasks.withType<GenerateModuleMetadata> {
    enabled = false
}
