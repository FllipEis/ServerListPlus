plugins {
    // Applies the correct Loom variant based on the Minecraft version
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
}

// DO NOT set group = ...!
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-fabric"

val requiredJava: JavaVersion = JavaVersion.toVersion(property("deps.java")!!)

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

sourceSets.main {
    // Version-independent core logic is compiled straight into every node
    java.srcDir(rootProject.file("core/src/main/java"))
}

dependencies {
    // Only pulls the Fabric API modules we actually use
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, sc.properties["deps.fabric_api"]))
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Applies Mojang mappings on obfuscated versions
    loomx.applyMojangMappings()

    // Use `mod{dependency type}` even on 26.1+ - loom-back-compat converts them
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    fapi("fabric-api-base", "fabric-lifecycle-events-v1", "fabric-networking-api-v1", "fabric-resource-loader-v0")
    // Players need the whole Fabric API (that is what fabric.mod.json asks for), so the dev client gets it too
    modLocalRuntime("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
}

// Client game tests start the real game and click through the screens (see src/gametest)
if (findProperty("mod.client_gametest") == "true") {
    fabricApi {
        configureTests {
            createSourceSet = true
            modId = "serverlistplus-gametest"
            enableGameTests = false
            enableClientGameTests = true
            eula = true
        }
    }
    sourceSets.named("gametest") {
        java.setSrcDirs(listOf(rootProject.file("src/gametest/java")))
        resources.setSrcDirs(listOf(rootProject.file("src/gametest/resources")))
    }
    dependencies {
        "gametestImplementation"(fabricApi.module("fabric-client-gametest-api-v1", sc.properties["deps.fabric_api"]))
    }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run") // Shared between versions
        jvmArguments.add("-Dmixin.debug.export=true")
    }
    // Game tests wipe their run directory before each run, so they must never share the one above
    runConfigs.matching { it.name == "clientGameTest" }.configureEach {
        runDirectory = project.file("build/run/clientGameTest")
        // `-PreadmeScreenshots` takes the README screenshots with real servers instead of the offline tests
        if (findProperty("readmeScreenshots") != null) jvmArguments.add("-Dserverlistplus.readmeScreenshots=true")
    }
}

// Uploads this jar on `./gradlew publishMods`. Without MODRINTH_TOKEN it is only a dry run.
publishMods {
    val modVersion = property("mod.version") as String
    val changelogFile = rootProject.file("CHANGELOG.md")
    val mcReleases = sc.properties.rawOrNull("mod", "mc_releases")?.asList().orEmpty().map { it.toString() }
    val curseforgeId = sc.properties.get<String>("publish.curseforge_id")
    val mcRange = if (mcReleases.size == 1) mcReleases.single() else "${mcReleases.first()}–${mcReleases.last()}"

    file = loomx.modJar.flatMap { it.archiveFile }
    displayName = "${property("mod.name")} $modVersion for Fabric $mcRange"
    version = "$modVersion+${sc.current.version}-fabric"
    changelog = provider { changelogSection(changelogFile, modVersion) }
    type = STABLE
    modLoaders.add("fabric")
    dryRun = providers.environmentVariable("MODRINTH_TOKEN").orNull == null

    modrinth {
        projectId = sc.properties.get<String>("publish.modrinth_id")
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(mcReleases)
        environment = CLIENT_ONLY
        requires("fabric-api")
    }
    if (curseforgeId.isNotBlank()) curseforge {
        projectId = curseforgeId
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.addAll(mcReleases)
        client = true
        server = false
        requires("fabric-api")
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, value: String) {
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", sc.properties["mod.id"])
            register("name", sc.properties["mod.name"])
            register("version", sc.properties["mod.version"])
            register("minecraft", sc.properties["mod.mc_compat"])
            register("loader", Regex("\\d+\\.\\d+").find(sc.properties.get<String>("deps.fabric_loader"))!!.value)
        }

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        exclude("META-INF/neoforge.mods.toml")
    }

    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from("../../LICENSE") { rename { "${it}_$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
