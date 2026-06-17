import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    id("io.papermc.paperweight.patcher") version "2.0.0-beta.17"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

paperweight {
    upstreams.paper {
        ref.set(providers.gradleProperty("paperCommit"))

        patchFile {
            path.set("paper-server/build.gradle.kts")
            outputFile.set(file("purpur-server/build.gradle.kts"))
            patchFile.set(file("purpur-server/build.gradle.kts.patch"))
        }
        patchFile {
            path.set("paper-api/build.gradle.kts")
            outputFile.set(file("purpur-api/build.gradle.kts"))
            patchFile.set(file("purpur-api/build.gradle.kts.patch"))
        }
        patchDir("paperApi") {
            upstreamPath.set("paper-api")
            excludes.set(setOf("build.gradle.kts"))
            patchesDir.set(file("purpur-api/paper-patches"))
            outputDir.set(file("paper-api"))
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
        options.isFork = true
        options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-removal"))
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }
    tasks.withType<Test> {
        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.STANDARD_OUT)
        }
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
        maven("https://jitpack.io")
    }

    extensions.configure<PublishingExtension> {
        repositories {
            maven("https://repo.purpurmc.org/snapshots") {
                name = "purpur"
                credentials(PasswordCredentials::class)
            }
        }
    }
}

tasks.register("printSWMVersion") {
    doLast {
        println("SWM dependencies added successfully")
    }
}

// Add SWM dependencies
subprojects {
    dependencies {
        // Only add to purpur-server (index 1) since it needs flow-nbt and zstd
        if (project.name == "purpur-server") {
            implementation("com.flowpowered:flow-nbt:2.0.0-1")
            implementation("com.github.luben:zstd-jni:1.5.5-11")
            compileOnly("org.projectlombok:lombok:1.18.30")
            annotationProcessor("org.projectlombok:lombok:1.18.30")
        }
        
        // Common dependencies for both api and server
        testImplementation("junit:junit:4.13.2")
    }
}

tasks.register("printMinecraftVersion") {
    doLast {
        println(providers.gradleProperty("mcVersion").get().trim())
    }
}

tasks.register("printPurpurVersion") {
    doLast {
        println(project.version)
    }
}
