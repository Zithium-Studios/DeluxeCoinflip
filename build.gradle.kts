plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
}

group = "net.zithium"
version = "2.11.3"
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
    implementation("com.mysql:mysql-connector-j:9.6.0")

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.12.2")
    compileOnly("com.mojang:authlib:1.5.21")
    compileOnly("org.jetbrains:annotations:26.0.2-1")

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

tasks {
    build {
        dependsOn("shadowJar")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        minimize {
            exclude(dependency("com.tcoded:FoliaLib:.*"))
        }

        dependencies {
            exclude(dependency("net.kyori:.*"))
        }

        exclude("net/kyori/**")

        archiveFileName.set("DeluxeCoinflip-${project.version}.jar")
        relocate("dev.triumphteam.gui", "net.zithium.deluxecoinflip.libs.gui")
        relocate("net.zithium.library", "net.zithium.deluxecoinflip.libs.library")
        relocate("com.tcoded.folialib", "net.zithium.deluxecoinflip.libs.folialib")
        relocate("org.bstats", "net.zithium.deluxecoinflip.libs.metrics") // bStats
    }
}
