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

	// 3D skin layers - bundled rather than reimplemented, see https://github.com/tr7zw/3d-skin-layers.
	// Compile-time only, for the mod menu toggle/options integration; the mod itself ships as its
	// own jar via launcher/mods-bundle, same as Continuity above.
	modImplementation("maven.modrinth:3dskinlayers:1.11.2")

	// Sodium - bundled rather than reimplemented, see https://github.com/CaffeineMC/sodium.
	// Compile-time only, for the "External Mods" mod-menu shortcut into Sodium's own settings
	// screen; the mod itself ships as its own jar via launcher/mods-bundle, same as Continuity
	// and 3D Skin Layers above. Version pinned to exactly the jar in mods-bundle (mc1.21.11-0.8.13).
	modImplementation("maven.modrinth:sodium:mc1.21.11-0.8.13-fabric")
}

tasks.processResources {
	inputs.property("version", project.version)

	// filteringCharset wasn't previously set explicitly - JDK 21 already defaults to UTF-8, but
	// fabric.mod.json now contains its first non-ASCII character (the "§" cape-URL token below),
	// so pin this rather than rely on the platform default.
	filteringCharset = "UTF-8"

	filesMatching("fabric.mod.json") {
		expand(mapOf("version" to project.version))
	}

	// NOTE on fabric.mod.json's "custom.cape.url": deliberately uses "§idNoHyphen" as the
	// placeholder token, not "$idNoHyphen" - the `expand()` call above runs the *entire* file
	// through Groovy's SimpleTemplateEngine, which treats any literal "$identifier" as a template
	// reference and would either fail the build (MissingPropertyException) or silently substitute
	// something wrong. "§" isn't special to that engine, so it survives expand() untouched and
	// Cape Provider reads it as its own per-player placeholder instead (see its README - "$" needs
	// escaping there for exactly this reason, "§" is its documented alternative).
	// Also, the bucket host/name in that URL is a placeholder (see launcher/.env.example) until the
	// real Backblaze B2 bucket exists - update both together once it does.
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
