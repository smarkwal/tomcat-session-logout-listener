import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.util.*

plugins {
    `java-library`
    jacoco
    signing
    `maven-publish`

    // run SonarQube analysis
    id("org.sonarqube") version "3.5.0.2730"

    // publish to Sonatype OSSRH and release to Maven Central
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"

    // get current Git branch name
    id("org.ajoberstar.grgit") version "5.0.0"

    // provide utility task "taskTree" for analysis of task dependencies
    id("com.dorongold.task-tree") version "2.1.0"
}

group = "net.markwalder"
description = "Tomcat valve for remote session invalidation."

val developerUrl = "https://github.com/smarkwal/"
val projectUrl = developerUrl + project.name

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

if (!JavaVersion.current().isJava11Compatible) {
    val error = "Build requires Java 11 and does not run on Java ${JavaVersion.current().majorVersion}."
    throw GradleException(error)
}

// Preconditions based on which tasks should be executed -----------------------

gradle.taskGraph.whenReady {

    // if sonarqube task should be executed ...
    if (gradle.taskGraph.hasTask(":sonarqube")) {
        // environment variable SONAR_TOKEN or property "sonar.login" must be set
        val tokenFound = project.hasProperty("sonar.login") || System.getenv("SONAR_TOKEN") != null
        if (!tokenFound) {
            val error = "SonarQube: Token not found.\nPlease set property 'sonar.login' or environment variable 'SONAR_TOKEN'."
            throw GradleException(error)
        }
    }

}

// dependencies ----------------------------------------------------------------

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {

    // main dependencies -------------------------------------------------------

    api("org.apache.tomcat:tomcat-catalina:9.0.70")

    // test dependencies -------------------------------------------------------

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")

    // Mockito
    testImplementation("org.mockito:mockito-core:4.9.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.9.0")

    // AssertJ
    testImplementation("org.assertj:assertj-core:3.23.1")

    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:1.17.6")
    testImplementation("org.testcontainers:junit-jupiter:1.17.6")

    // Apache HttpClient
    testImplementation("org.apache.httpcomponents:httpclient:4.5.14")
    testImplementation("org.apache.httpcomponents:fluent-hc:4.5.14")

    // Apache Commons IO
    testImplementation("commons-io:commons-io:2.11.0")

    // SLF4J
    testImplementation("org.slf4j:slf4j-api:2.0.6")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.5")
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

tasks.withType<Jar> {
    manifest {
        attributes["Version"] = project.version
    }
}

tasks.getByName<Test>("test") {

    // make sure that JAR file has been built before tests are executed
    dependsOn("jar")

    // pass JAR file path to tests as system property
    val archiveFilePath = tasks.jar.get().archiveFile.get().asFile.absolutePath
    systemProperty("project.archiveFilePath", archiveFilePath)

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

tasks {

    jacocoTestReport {

        // run all tests first
        dependsOn(test)

        // get JaCoCo data from all test tasks
        executionData.from(
            "${buildDir}/jacoco/test.exec"
        )

        reports {

            // generate XML report (required for SonarQube)
            xml.required.set(true)
            xml.outputLocation.set(file("${buildDir}/reports/jacoco/test/report.xml"))

            // generate HTML report
            html.required.set(true)

            // generate CSV report
            // csv.required.set(true)
        }
    }

}

sonarqube {
    // documentation: https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-gradle/

    properties {

        // connection to SonarCloud
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", "smarkwal")
        property("sonar.projectKey", "smarkwal_tomcat-session-logout-listener")

        // Git branch
        property("sonar.branch.name", getGitBranchName())

        // paths to test sources and test classes
        property("sonar.tests", "${projectDir}/src/test/java")
        property("sonar.java.test.binaries", "${buildDir}/classes/java/test")

        // include test results
        property("sonar.junit.reportPaths", "${buildDir}/test-results/test")

        // include test coverage results
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", "${buildDir}/reports/jacoco/test/report.xml")
    }
}

tasks.sonarqube {
    // run all tests and generate JaCoCo XML report
    dependsOn(
        tasks.test,
        tasks.jacocoTestReport
    )
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

                name.set(project.name)
                description.set(project.description)
                url.set(projectUrl)

                licenses {
                    license {
                        name.set("MIT")
                        url.set("$projectUrl/blob/main/LICENSE")
                    }
                }

                developers {
                    developer {
                        id.set("smarkwal")
                        name.set("Stephan Markwalder")
                        email.set("stephan@markwalder.net")
                        url.set(developerUrl)
                    }
                }

                scm {
                    connection.set(projectUrl.replace("https://", "scm:git:git://") + ".git")
                    developerConnection.set(projectUrl.replace("https://", "scm:git:ssh://") + ".git")
                    url.set(projectUrl)
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

// helper functions ------------------------------------------------------------

fun getGitBranchName(): String {
    return grgit.branch.current().name
}
