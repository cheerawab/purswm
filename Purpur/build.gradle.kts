plugins {
    id("java-library")
    id("maven-publish")
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

group = "org.purpurmc.purpur"
version = "26.1.2-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    
    dependencies {
        // Lombok annotation processing - shared across all projects
        compileOnly("org.projectlombok:lombok:1.18.30")
        annotationProcessor("org.projectlombok:lombok:1.18.30")
    }
    
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
    
    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.isFork = true
        options.compilerArgs.add("-Xlint:-deprecation")
        // Enable Lombok annotation processing
        options.annotationProcessorPath = configurations.getByName("annotationProcessor")
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

tasks.register("printSWMVersion") {
    doLast {
        println("SWM dependencies added successfully")
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
