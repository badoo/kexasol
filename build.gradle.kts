import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

group = "com.badoo.kexasol"
version = "0.2.1"

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("kapt") version "1.6.10"
    `java-library`
    `maven-publish`
    id("com.github.ben-manes.versions") version ("0.42.0")
}

/* ./gradlew dependencyUpdates */
tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    checkForGradleUpdate = true
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs += "-Xjvm-default=all-compatibility"
        }
    }

    test {
        useTestNG()
        maxParallelForks = 1

        if (project.hasProperty("EXADEBUG")) {
            jvmArgs = listOf("-Dorg.slf4j.simpleLogger.defaultLogLevel=debug")
        }

        testLogging {
            showStandardStreams = true
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    api("com.squareup.okio:okio:3.0.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.10")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.moshi:moshi-kotlin:1.13.0")
    implementation("com.univocity:univocity-parsers:2.9.1")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("org.slf4j:slf4j-api:1.7.36")

    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.13.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test-common:1.6.10")
    testImplementation("org.jetbrains.kotlin:kotlin-test-annotations-common:1.6.10")
    testImplementation("org.jetbrains.kotlin:kotlin-test-testng:1.6.10")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.slf4j:slf4j-simple:1.7.36")
}

kotlin {
    sourceSets["main"].apply {
        kotlin.srcDir("kexasol")
    }

    sourceSets["test"].apply {
        kotlin.srcDir("examples")
    }
}

publishing {
    publications {
        create<MavenPublication>("kexasol") {
            from(components["java"])
        }
    }
}
