import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "net.artemisitor.server"
version = "1.0.0"

repositories {
    maven("https://repo.huaweicloud.com/repository/maven/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    testImplementation(kotlin("test"))
}

application {
    mainClass = "net.artemisitor.server.Main"
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<JavaExec> {
    systemProperty("file.encoding", "utf-8")
}

// 配置 Shadow JAR 任务
tasks.withType<ShadowJar> {
    archiveClassifier.set("all")
    manifest {
        attributes(
            "Premain-Class" to "net.artemisitor.server.MyAgent",
            "Main-Class" to application.mainClass
        )
    }
    mergeServiceFiles()
}