pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
    plugins {
        // Uploads the jars to Modrinth and CurseForge, see `publishMods` in the loader build scripts
        id("me.modmuss50.mod-publish-plugin") version "2.2.1"
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Applies the right Loom variant for obfuscated (<=1.21.11) and unobfuscated (26.1+) versions
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Version-independent logic (data model, storage, filter/sort). Plain Java, unit-tested.
include("core")

stonecutter {
    create(rootProject) {
        /**
         * Creates `versions/{project}-{loader}` nodes, each using `build.{loader}.gradle.kts`.
         * A node covers a range of Minecraft releases (see `mod.mc_compat` in stonecutter.properties.toml).
         * Only add a new node when an existing range stops compiling or a mixin breaks.
         */
        fun match(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        match("1.20.6", "fabric", "neoforge")
        match("1.21.1", "fabric", "neoforge")
        match("1.21.3", "fabric", "neoforge")
        match("1.21.4", "fabric", "neoforge")
        match("1.21.5", "fabric", "neoforge")
        match("1.21.8", "fabric", "neoforge")
        match("1.21.10", "fabric", "neoforge")
        match("1.21.11", "fabric", "neoforge")
        match("26.1.2", "fabric", "neoforge")
        match("26.2", "fabric", "neoforge")
        match("26.3", "fabric", "neoforge")
        vcsVersion = "26.3-fabric"
    }
}

rootProject.name = "ServerListPlus"
