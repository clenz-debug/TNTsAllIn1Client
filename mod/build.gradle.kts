val minecraft_version: String by project
val loader_version: String by project
val fabric_api_version: String by project

plugins {
	id("net.fabricmc.fabric-loom-remap")
	`maven-publish`
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

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
	// see https://github.com/PepperCode1/Continuity. Beta build - only release supporting 1.21.11.
	modImplementation("maven.modrinth:continuity:3.0.1-beta.1+1.21.11")
}

tasks.processResources {
	inputs.property("version", project.version)

	filesMatching("fabric.mod.json") {
		expand(mapOf("version" to project.version))
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(21)
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
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
