plugins {
    kotlin("jvm") version "1.3.61"
    id("org.jetbrains.dokka") version "0.10.0"
    application
    jacoco
    antlr
}

group = "net.bms.orwell"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
}

application {
    mainClassName = "$group.OrwellKt"
}

jacoco {
    toolVersion = "0.8.5"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    antlr("org.antlr:antlr4:4.8")
    compile("com.github.ftomassetti:kllvm:0.1.0")
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
        dependsOn("generateGrammarSource")
        kotlinOptions.suppressWarnings = true
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    dokka {
        outputFormat = "html"
        outputDirectory = "$buildDir/dokka/html"
    }
    generateGrammarSource {
        outputDirectory = File("$buildDir/generated-src/antlr/main/java")
    }
    register("dokkaMarkdown", org.jetbrains.dokka.gradle.DokkaTask::class) {
        outputFormat = "gfm"
        outputDirectory = "$buildDir/dokka/gfm"
    }
}

sourceSets {
    main {
        java.srcDir("$buildDir/generated-src/antlr/main/java")
    }
}