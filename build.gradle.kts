plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.6"
}

group = "net.zithium"
version = "2.9.6"
description = "DeluxeCoinflip"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://ci.ender.zone/plugin/repository/everything/")
    maven("https://raw.githubusercontent.com/TeamVK/maven-repository/master/release/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://libraries.minecraft.net/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.rosewooddev.io/repository/public/")
}

dependencies {
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("dev.triumphteam:triumph-gui:3.1.11")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("com.github.ItzSave:ZithiumLibrary:v2.1.2")
    implementation("com.github.NahuLD.folia-scheduler-wrapper:folia-scheduler-wrapper:v0.0.3")

    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.mojang:authlib:1.5.21")
    compileOnly("org.jetbrains:annotations:26.0.2")

    compileOnly("com.github.Realizedd:TokenManager:3.2.4") { isTransitive = false }
    compileOnly("net.milkbowl.vault:VaultAPI:1.7")
    compileOnly("org.black_ixx:playerpoints:3.2.6")
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    minimize {
        exclude(dependency("com.github.NahuLD.folia-scheduler-wrapper:folia-scheduler-wrapper:.*"))
    }

    archiveFileName.set("DeluxeCoinflip-${project.version}.jar")
    relocate("dev.triumphteam.gui", "net.zithium.deluxecoinflip.libs.gui")
    relocate("net.zithium.library", "net.zithium.deluxecoinflip.libs.library")
    relocate("org.bstats", "net.zithium.deluxecoinflip.libs.metrics") // bStats
}
