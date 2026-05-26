plugins {
    // This plugin applies the correct loom variant based on the Minecraft version
    id("dev.kikugie.loom-back-compat")
}

// DO NOT set group = ...!
val archiveMinecraftVersion: String = sc.properties["mod.mc_archive"]
version = "${property("mod.version")}+$archiveMinecraftVersion"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

// This can be used for publishing on Modrinth and Curseforge
val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Applies Mojang Mappings on obfuscated versions
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
        }

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    val requestedTaskNames = gradle.startParameter.taskNames.map { it.substringAfterLast(':') }
    val buildSourcesJar = requestedTaskNames.any {
        it == "sourcesJar" || it == "remapSourcesJar" || it.startsWith("publish")
    }
    val buildDevJar = requestedTaskNames.any { it == "jar" }

    matching { it.name == "sourcesJar" || it.name == "remapSourcesJar" }.configureEach {
        onlyIf("sources jars are requested explicitly or for publishing") { buildSourcesJar }
    }

    named<AbstractArchiveTask>("jar") {
        if (findByName("remapJar") == null) {
            destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
        } else {
            archiveClassifier = if (buildDevJar) "dev" else "remap-input"
            destinationDirectory = layout.buildDirectory.dir("devlibs")
        }
    }

    withType<AbstractArchiveTask>().matching {
        it.name == "remapJar" || it.name == "remapSourcesJar"
    }.configureEach {
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
    }
}
