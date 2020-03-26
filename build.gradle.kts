plugins {
    kotlin("jvm") version "1.3.71"
    id("org.jetbrains.dokka") version "0.10.0"
    application
    jacoco
    antlr
}

group = "net.bms.orwell"
version = "0.1.0"

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
    named("run", JavaExec::class) {
        this.standardInput = System.`in`
    }
    jar {
        manifest{
            attributes["Implementation-Title"] = project.name
            attributes["Implementation-Version"] = project.version
            attributes["Main-Class"] = application.mainClassName
        }
        from(configurations.compile.get().map {
            if (it.isDirectory) it else zipTree(it)
        })
    }
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
    processResources {
        filter{ it.replace("%VERSION%", project.version.toString()) }
    }
}

sourceSets {
    main {
        java.srcDir("$buildDir/generated-src/antlr/main/java")
    }
}