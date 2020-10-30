import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

group = "com.badoo.kexasol"
version = "0.2.1"

plugins {
    kotlin("jvm") version "1.4.10"
    kotlin("kapt") version "1.4.10"
    `java-library`
    `maven-publish`
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            noReflect = true
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
    api("com.squareup.okio:okio:2.9.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.10")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.11.0")
    implementation("com.univocity:univocity-parsers:2.9.0")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.66")
    implementation("org.slf4j:slf4j-api:1.7.30")

    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.11.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test-common:1.4.10")
    testImplementation("org.jetbrains.kotlin:kotlin-test-annotations-common:1.4.10")
    testImplementation("org.jetbrains.kotlin:kotlin-test-testng:1.4.10")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
    testImplementation("org.slf4j:slf4j-simple:1.7.30")
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
