import java.util.Properties

plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://libraries.githubusercontent.com/incompatible/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2-R0.1-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    implementation("com.flowpowered:flow-nbt:2.0.0-1")
    implementation("com.github.luben:zstd-jni:1.5.5-11")
}

tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()
    options.isFork = true
    options.compilerArgs.add("-Xlint:-deprecation")
}
