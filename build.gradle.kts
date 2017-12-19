import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

import org.junit.platform.gradle.plugin.FiltersExtension
import org.junit.platform.gradle.plugin.EnginesExtension
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

group = "co.petrin.kotlin"
version = "0.5-SNAPSHOT"

buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.2.0"

    repositories {
        maven {
            setUrl("http://dl.bintray.com/kotlin/kotlin-eap-1.2")
        }
        mavenCentral()
    }
    
    dependencies {
        classpath(kotlinModule("gradle-plugin", kotlin_version))
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.0.0")
    }
    
}

plugins {
    java
}

apply {
    plugin("application")
    plugin("kotlin")
    plugin("org.junit.platform.gradle.plugin")
}

/*configure {
    filters {
        engines {
            include("spek")
        }
    }
}*/

val kotlin_version: String by extra

repositories {
    maven {
        setUrl("http://dl.bintray.com/kotlin/kotlin-eap-1.2")
    }
    mavenCentral()
}

dependencies {
    compileOnly(kotlinModule("stdlib-jdk8", kotlin_version))
    compileOnly("io.ratpack", "ratpack-core", "1.5.0")
    compileOnly("io.ratpack", "ratpack-exec", "1.5.0")
    compileOnly("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "0.20")

    testCompile(kotlinModule("stdlib-jdk8", kotlin_version))
    testCompile("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "0.20")
    testCompile("io.ratpack", "ratpack-core", "1.5.0")
    testCompile("io.ratpack", "ratpack-exec", "1.5.0")
    testCompile("io.ratpack", "ratpack-test", "1.5.0")
    testCompile("com.natpryce", "hamkrest","1.4.2.2")
    testCompile("org.jetbrains.kotlin", "kotlin-reflect", kotlin_version)
    testCompile("ch.qos.logback", "logback-classic", "1.2.3")
    testCompile("org.jetbrains.spek:spek-api:1.1.5") {
        exclude("org.jetbrains.kotlin")
    }
    testRuntime("org.jetbrains.spek:spek-junit-platform-engine:1.1.5") {
        exclude("org.jetbrains.kotlin")
        exclude("org.junit.platform")
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}


// extension for Spek configuration
fun JUnitPlatformExtension.filters(setup: FiltersExtension.() -> Unit) {
    when (this) {
        is ExtensionAware -> extensions.getByType(FiltersExtension::class.java).setup()
        else -> throw Exception("${this::class} must be an instance of ExtensionAware")
    }
}
fun FiltersExtension.engines(setup: EnginesExtension.() -> Unit) {
    when (this) {
        is ExtensionAware -> extensions.getByType(EnginesExtension::class.java).setup()
        else -> throw Exception("${this::class} must be an instance of ExtensionAware")
    }
}
