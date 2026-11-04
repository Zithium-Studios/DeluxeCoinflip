import org.gradle.api.file.DuplicatesStrategy

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
}

group = "net.zithium"
version = "2.11.4"
description = "DeluxeCoinflip"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://ci.ender.zone/plugin/repository/everything/")
    maven("https://raw.githubusercontent.com/TeamVK/maven-repository/master/release/")
    maven("https://jitpack.io")
    maven("https://repo.tcoded.com/releases/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://libraries.minecraft.net/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.rosewooddev.io/repository/public/")
}

dependencies {
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("dev.triumphteam:triumph-gui:3.1.13")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("com.github.ItzSave:ZithiumLibrary:1f5182b77f")
    implementation("com.tcoded:FoliaLib:0.5.1")

    implementation(platform("net.kyori:adventure-bom:4.22.0"))
    implementation("net.kyori:adventure-api")
    implementation("net.kyori:adventure-text-minimessage")
    implementation("net.kyori:adventure-platform-bukkit:4.4.1")

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.12.3")
    compileOnly("com.mojang:authlib:1.5.21")
    compileOnly("org.jetbrains:annotations:26.0.2-1")

    compileOnly("com.github.Realizedd:TokenManager:3.2.4") {
        isTransitive = false
    }
    compileOnly("net.milkbowl.vault:VaultAPI:1.7")
    compileOnly("org.black_ixx:playerpoints:3.2.6")
    compileOnly(
        fileTree(
            mapOf(
                "dir" to "libs",
                "include" to listOf("*.jar")
            )
        )
    )
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks {
    jar {
        enabled = false
    }

    build {
        dependsOn("shadowJar")
    }

    processResources {
        val props = mapOf("version" to project.version)

        inputs.properties(props)
        filteringCharset = "UTF-8"

        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveFileName.set("DeluxeCoinflip-${project.version}.jar")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        mergeServiceFiles()

        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        relocate("dev.triumphteam.gui", "net.zithium.deluxecoinflip.libs.gui")
        relocate("net.zithium.library", "net.zithium.deluxecoinflip.libs.library")
        relocate("com.tcoded.folialib", "net.zithium.deluxecoinflip.libs.folialib")
        relocate("org.bstats", "net.zithium.deluxecoinflip.libs.metrics")
        relocate("net.kyori", "net.zithium.deluxecoinflip.libs.kyori")
    }
}