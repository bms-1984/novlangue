@file:Suppress("KDocMissingDocumentation")

plugins {
    kotlin("jvm") version "1.4.0"
    id("org.jetbrains.dokka") version "1.4.0-rc"
    id("com.diffplug.spotless") version "5.3.0"
    application
    jacoco
    antlr
    `maven-publish`
}

group = "net.bms.novlangue"
version = "0.1.5"

val ktlintVersion: String = "0.38.1"
val kllvmVersion: String = "0.1.6-SNAPSHOT"
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
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/bms-1984/kllvm")
            credentials {
                username = System.getenv("GHUSERNAME")
                password = System.getenv("GHTOKEN")
            }
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/bms-1984/novlangue")
            credentials {
                username = System.getenv("GHUSERNAME")
                password = System.getenv("GHTOKEN")
            }
        }
    }
    publications { create<MavenPublication>("novlangue") { from(components["java"]) } }
}

application {
    mainClassName = "$group.NovlangueKt"
}

spotless {
    kotlin {
        ktlint(ktlintVersion)
        licenseHeader("/* (C) Ben M. Sutter \$YEAR */", "^package|(.+Novlangue Test Suite.+)")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(ktlintVersion)
    }
    antlr4 {
        target("src/main/antlr/*.g4")
        antlr4Formatter(antlr4FormatterVersion)
        licenseHeader("/* (C) Ben M. Sutter \$YEAR */")
    }
}

jacoco {
    toolVersion = jacocoVersion
}

dependencies {
    antlr("org.antlr:antlr4:$antlr4Version")
    implementation(kotlin("reflect"))
    implementation("me.tomassetti:kllvm:$kllvmVersion")
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
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "13"
            useIR = true
            languageVersion = "1.4"
            apiVersion = languageVersion
            verbose = true
            allWarningsAsErrors = true
            freeCompilerArgs = freeCompilerArgs + "-progressive"
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
}

sourceSets.main {
    java.srcDir("$buildDir/generated-src/antlr/main/java")
}
