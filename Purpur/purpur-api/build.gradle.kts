plugins {
    `java-library`
    `maven-publish`
}

group = "org.purpurmc.purpur"
version = "26.1.2-SNAPSHOT"

dependencies {
    // Bukkit API as compileOnly (provided by the server)
    compileOnly("io.papermc.paper:paper-api:26.1.2-R0.1-SNAPSHOT")

    // Lombok annotation processing
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // NBT library
    implementation("com.flowpowered:flow-nbt:2.0.0-1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()
    options.isFork = true
    options.compilerArgs.addAll(listOf("-Xlint:-deprecation"))
    // Enable Lombok annotation processing    
}

tasks.withType<ProcessResources> {
    filteringCharset = Charsets.UTF_8.name()
}
