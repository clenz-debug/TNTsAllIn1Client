val minecraft_version: String by project
val loader_version: String by project
val fabric_api_version: String by project
val java_version: String by project

plugins {
	id("net.fabricmc.fabric-loom-remap")
	`maven-publish`
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

loom {
	// The model cube internals the 3D skin layers read - public from 1.21.2 on.
	accessWidenerPath.set(file("src/main/resources/tntsallin1client.accesswidener"))
}

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	maven("https://api.modrinth.com/maven") {
		name = "Modrinth"
		content { includeGroup("maven.modrinth") }
	}
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:$minecraft_version")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:$loader_version")

	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_api_version")

	// Phase 5f: connected textures (3D glass etc.) - bundled rather than reimplemented,
	// see https://github.com/PepperCode1/Continuity. Its release for 1.20.5 and 1.20.6.
	modImplementation("maven.modrinth:continuity:3.0.0+1.20.5")

	// Sodium - bundled rather than reimplemented, see https://github.com/CaffeineMC/sodium.
	// Compile-time only, for the "External Mods" mod-menu shortcut into Sodium's own settings
	// screen; the mod itself ships as its own jar via launcher/mods-bundle, same as Continuity
	// above. Version pinned to exactly the jar in mods-bundle (mc1.20.6-0.5.11).
	modImplementation("maven.modrinth:sodium:mc1.20.6-0.5.11")
}

tasks.processResources {
	inputs.property("version", project.version)
	inputs.property("minecraft_version", minecraft_version)
	inputs.property("java_version", java_version)

	// Pinned rather than relying on the platform default.
	filteringCharset = "UTF-8"

	filesMatching("fabric.mod.json") {
		expand(mapOf("version" to project.version, "minecraft_version" to minecraft_version, "java_version" to java_version))
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(java_version.toInt())
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	withSourcesJar()

	val javaVersionEnum = JavaVersion.toVersion(java_version)
	sourceCompatibility = javaVersionEnum
	targetCompatibility = javaVersionEnum
}

// configure the maven publication
publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	repositories {
		// Add repositories to publish to here.
	}
}
