plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "net.artemisitor.server"
repositories {
    maven("https://repo.huaweicloud.com/repository/maven/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    implementation("com.google.code.gson:gson:2.8.9")
}
application{
    mainClass = "net.artemisitor.server.Main"
}