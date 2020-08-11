plugins {
    kotlin("jvm") version "1.4.0-rc"
    id("org.jetbrains.dokka") version "0.10.1"
    application
    jacoco
    antlr
    `maven-publish`
    maven
}

group = "net.bms.orwell"
version = "0.1.3"

repositories {
    mavenCentral()
    jcenter()
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
            url = uri("https://maven.pkg.github.com/bms-1984/orwell")
            credentials {
                username = System.getenv("GHUSERNAME")
                password = System.getenv("GHTOKEN")
            }
        }
    }
    publications { create<MavenPublication>("orwell") { from(components["java"]) } }
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
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.12")
    implementation("me.tomassetti:kllvm:0.1.3-SNAPSHOT")

}

tasks {
    named("run", JavaExec::class) {
        this.standardInput = System.`in`
    }
    jar {
        manifest {
            attributes["Implementation-Title"] = project.name
            attributes["Implementation-Version"] = project.version
            attributes["Main-Class"] = application.mainClassName
        }
        from(configurations.compileClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        })
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "13"
        dependsOn("generateGrammarSource")
        kotlinOptions.suppressWarnings = true
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "13"
    }
    dokka {
        outputFormat = "html"
        outputDirectory = "$buildDir/dokka/html"
    }
    generateGrammarSource {
        outputDirectory = File("$buildDir/generated-src/antlr/main/java")
        arguments.add("-visitor")
        arguments.add("-no-listener")
    }
    register("dokkaMarkdown", org.jetbrains.dokka.gradle.DokkaTask::class) {
        outputFormat = "gfm"
        outputDirectory = "$buildDir/dokka/gfm"
    }
    processResources {
        filter { it.replace("%VERSION%", project.version.toString()) }
    }
    register("version") {
        doLast {
            println("Version $version")
        }
    }
}

sourceSets.main {
    java.srcDir("$buildDir/generated-src/antlr/main/java")
}