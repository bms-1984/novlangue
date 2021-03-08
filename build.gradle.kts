@file:Suppress("KDocMissingDocumentation")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.0"
    id("org.jetbrains.dokka") version "1.4.0-rc"
    id("com.diffplug.spotless") version "5.11.0"
    application
    jacoco
    antlr
    `maven-publish`
}

group = "net.bms.novlangue"
version = "0.1.6"

val production: Boolean = false
val ktlintVersion: String = "0.38.1"
val llvmVersion: String = "10.0.0-1.5.3"
val jacocoVersion: String = "0.8.5"
val antlr4Version: String = "4.8"
val antlr4FormatterVersion: String = "1.2.1"

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/kotlin/dokka")
    maven("https://jitpack.io")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://kotlin.bintray.com/kotlinx")
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/bms-1984/novlangue")
            credentials {
                username = "bms-1984"
                password = System.getenv("GHTOKEN")
            }
        }
    }
    publications { create<MavenPublication>("novlangue") { from(components["java"]) } }
}

application {
    mainClassName = "$group.NovlangueKt"
    applicationName = "Novlangue"
}

spotless {
    kotlin {
        ktlint(ktlintVersion)
        licenseHeaderFile(file("docs/LICENSE_HEADER"), "^package|(.+Novlangue Test Suite.+)")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(ktlintVersion)
    }
    antlr4 {
        target("src/main/antlr/*.g4")
        antlr4Formatter(antlr4FormatterVersion)
        licenseHeaderFile(file("docs/LICENSE_HEADER"))
    }
}

jacoco {
    toolVersion = jacocoVersion
}

dependencies {
    antlr("org.antlr:antlr4:$antlr4Version")
    implementation(kotlin("reflect"))
    implementation("org.bytedeco:llvm-platform:$llvmVersion")
}

kotlin {
    sourceSets {
        test {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}

tasks {
    named<JavaExec>("run") {
        workingDir("run")
        if (project.hasProperty("input.file"))
            args(project.properties["input.file"])
        if (project.hasProperty("main.false"))
            args("-noMain")
        if (project.hasProperty("stdlib.false"))
            args("-noStd")
    }
    jar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        manifest {
            attributes["Implementation-Title"] = project.name
            attributes["Implementation-Version"] = project.version
            attributes["Main-Class"] = application.mainClassName
        }
        from(
            configurations.compileClasspath.get().map {
                if (it.isDirectory) it else zipTree(it)
            }
        )
    }
    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "13"
            useIR = true
            languageVersion = "1.4"
            apiVersion = languageVersion
            verbose = true
            freeCompilerArgs = freeCompilerArgs + "-progressive"
            allWarningsAsErrors = production
        }
        dependsOn(generateGrammarSource)
    }
    generateGrammarSource {
        outputDirectory = File("$buildDir/generated-src/antlr/main/java")
        arguments.addAll(arrayOf("-visitor", "-no-listener"))
    }
    processResources {
        filter { it.replace("%VERSION%", project.version.toString()) }
    }
    register("version") {
        doLast {
            println("Version $version")
        }
    }
    dokkaHtml {
        outputDirectory = "$buildDir/dokka/html"
    }
    wrapper {
        gradleVersion = "6.6.1"
        distributionType = Wrapper.DistributionType.ALL
    }
    test {
        finalizedBy(jacocoTestReport)
    }
    jacocoTestReport {
        reports {
            csv.isEnabled = false
            xml.isEnabled = false
        }
        dependsOn(test)
    }
}

sourceSets.main {
    java.srcDir("$buildDir/generated-src/antlr/main/java")
}
