import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.util.*

plugins {
    `java-library`
    jacoco
    signing
    `maven-publish`

    // run Sonar analysis
    id("org.sonarqube") version "5.0.0.4638"

    // publish to Sonatype OSSRH and release to Maven Central
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"

    // get current Git branch name
    id("org.ajoberstar.grgit") version "5.2.2"

    // Gradle Versions Plugin
    // https://github.com/ben-manes/gradle-versions-plugin
    id("com.github.ben-manes.versions") version "0.51.0"

    // JarHC Gradle plugin
    id("org.jarhc") version "1.0.1"
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

if (!JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    val error = "Build requires Java 17 and does not run on Java ${JavaVersion.current().majorVersion}."
    throw GradleException(error)
}

// Preconditions based on which tasks should be executed -----------------------

gradle.taskGraph.whenReady {

    // if sonar task should be executed ...
    if (gradle.taskGraph.hasTask(":sonar")) {
        // environment variable SONAR_TOKEN or system property "sonar.token" must be set
        val tokenFound = System.getProperties().containsKey("sonar.token") || System.getenv("SONAR_TOKEN") != null
        if (!tokenFound) {
            val error = "Sonar: Token not found.\nPlease set system property 'sonar.token' or environment variable 'SONAR_TOKEN'."
            throw GradleException(error)
        }
    }

}

// dependencies ----------------------------------------------------------------

repositories {
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {

    // main dependencies -------------------------------------------------------

    api("org.apache.tomcat:tomcat-catalina:9.0.87")

    // test dependencies -------------------------------------------------------

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Mockito
    // Note: Mockito 5 is not compatible with Java 8.
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.11.0")

    // AssertJ
    testImplementation("org.assertj:assertj-core:3.25.3")

    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:1.19.7")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")

    // Apache HttpClient
    testImplementation("org.apache.httpcomponents:httpclient:4.5.14")
    testImplementation("org.apache.httpcomponents:fluent-hc:4.5.14")
    // fix Cxeb68d52e-5509 in transitive dependency on Commons Codec
    testImplementation("commons-codec:commons-codec:1.16.1")

    // Apache Commons IO
    testImplementation("commons-io:commons-io:2.16.0")

    // SLF4J
    testImplementation("org.slf4j:slf4j-api:2.0.12")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.12")
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
    toolVersion = "0.8.10"
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
            "${layout.buildDirectory.get()}/jacoco/test.exec"
        )

        reports {

            // generate XML report (required for Sonar)
            xml.required.set(true)
            xml.outputLocation.set(file("${layout.buildDirectory.get()}/reports/jacoco/test/report.xml"))

            // generate HTML report
            html.required.set(true)

            // generate CSV report
            // csv.required.set(true)
        }
    }

    jarhcReport {
        dependsOn(jar)
        classpath.setFrom(
            jar.get().archiveFile,
            configurations.runtimeClasspath
        )
        reportFiles.setFrom(
            file("${projectDir}/docs/jarhc-report.html"),
            file("${projectDir}/docs/jarhc-report.txt")
        )
    }

    build {
        dependsOn(jarhcReport)
    }

    dependencyUpdates {
        rejectVersionIf {
            candidate.version.contains("-M") // ignore milestone version
                    || candidate.version.contains("-rc") // ignore release candidate versions
                    || candidate.version.contains("-alpha") // ignore alpha versions
                    || candidate.group == "org.apache.tomcat" && candidate.module == "tomcat-catalina" && candidate.version < "9" // ignore Tomcat 10.0 and greater
                    || candidate.group == "org.mockito" && candidate.version >= "5" // ignore Mockito 5 and greater
        }
    }

}

sonar {
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
        property("sonar.java.test.binaries", "${layout.buildDirectory.get()}/classes/java/test")

        // include test results
        property("sonar.junit.reportPaths", "${layout.buildDirectory.get()}/test-results/test")

        // include test coverage results
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", "${layout.buildDirectory.get()}/reports/jacoco/test/report.xml")
    }
}

tasks.sonar {
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
    this.repositories {
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
