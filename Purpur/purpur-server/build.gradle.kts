plugins {
    id("java-library")
    id("maven-publish")
}

group = "org.purpurmc.purpur"
version = "26.1.2-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    // Lombok annotation processing
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    
    // NBT library for slime format
    implementation("com.flowpowered:flow-nbt:2.0.0-1")
    
    // Compression library for world data
    implementation("com.github.luben:zstd-jni:1.5.5-11")
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
}

tasks.withType<ProcessResources> {
    filteringCharset = Charsets.UTF_8.name()
}

tasks.register("printSWMVersion") {
    doLast {
        println("SWM dependencies added successfully")
    }
}
