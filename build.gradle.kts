plugins {
    kotlin("jvm") version "1.4.0"
    id("org.jetbrains.dokka") version "1.4.0-rc"
    application
    jacoco
    antlr
    `maven-publish`
}

group = "net.bms.novlangue"
version = "0.1.4"

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

jacoco {
    toolVersion = "0.8.5"
}

dependencies {
    antlr("org.antlr:antlr4:4.8")
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.12")
    implementation("me.tomassetti:kllvm:0.1.4-SNAPSHOT")

}

tasks {
    named("run", JavaExec::class) {
        standardInput = System.`in`
    }
    jar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
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
        kotlinOptions.useIR = true
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "13"
        dependsOn("generateGrammarSource")
        kotlinOptions.suppressWarnings = true
        kotlinOptions.useIR = true
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
}

sourceSets.main {
    java.srcDir("$buildDir/generated-src/antlr/main/java")
}