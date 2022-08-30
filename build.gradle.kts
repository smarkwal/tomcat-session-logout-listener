import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.util.*

plugins {
    java
    jacoco
    signing
    `maven-publish`

    // publish to Sonatype OSSRH and release to Maven Central
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

group = "net.markwalder"
description = "Tomcat value for remote session invalidation."

// load user-specific properties -----------------------------------------------

val userPropertiesFile = file("${projectDir}/gradle.user.properties")
if (userPropertiesFile.exists()) {
    val userProperties = Properties()
    userProperties.load(userPropertiesFile.inputStream())
    userProperties.forEach {
        project.ext.set(it.key.toString(), it.value)
    }
}

// Java version check ----------------------------------------------------------

if (!JavaVersion.current().isJava8Compatible) {
    val error = "Build requires Java 8 and does not run on Java ${JavaVersion.current().majorVersion}."
    throw GradleException(error)
}

// dependencies ----------------------------------------------------------------

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {

    // main dependencies
    implementation("org.apache.tomcat:tomcat-catalina:9.0.65")

    // test dependencies
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

publishing {
    publications {
        create<MavenPublication>("maven") {

            from(components["java"])

            pom {

                name.set("tomcat-session-logout-listener")
                description.set("Tomcat value for remote session invalidation.")
                url.set("https://github.com/smarkwal/tomcat-session-logout-listener")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/smarkwal/tomcat-session-logout-listener/blob/main/LICENSE")
                    }
                }

                developers {
                    developer {
                        id.set("smarkwal")
                        name.set("Stephan Markwalder")
                        email.set("stephan@markwalder.net")
                        url.set("https://github.com/smarkwal")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/smarkwal/tomcat-session-logout-listener.git")
                    developerConnection.set("scm:git:ssh://github.com/smarkwal/tomcat-session-logout-listener.git")
                    url.set("https://github.com/smarkwal/tomcat-session-logout-listener")
                }

            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}
