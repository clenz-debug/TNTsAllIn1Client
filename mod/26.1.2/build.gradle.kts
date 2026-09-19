val minecraft_version: String by project
val loader_version: String by project
val fabric_api_version: String by project
val java_version: String by project

plugins {
	// 26.x ships unobfuscated - no more remap step, so the plain plugin id instead of the
	// "-remap" alias used for the still-obfuscated 1.21.11 build (see gradle.properties).
	id("net.fabricmc.fabric-loom")
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
	// No mappings() call - 26.x ships unobfuscated, there is nothing left to map.
	implementation("net.fabricmc:fabric-loader:$loader_version")

	// Fabric API. This is technically optional, but you probably want it anyway.
	implementation("net.fabricmc.fabric-api:fabric-api:$fabric_api_version")

	// Phase 5f: connected textures (3D glass etc.) - bundled rather than reimplemented,
	// see https://github.com/PepperCode1/Continuity. Beta build - matches launcher/mods-bundle/26.1.2/.
	compileOnly("maven.modrinth:continuity:3.0.1-beta.2+26.1")

	// 3D skin layers - bundled rather than reimplemented, see https://github.com/tr7zw/3d-skin-layers.
	// Compile-time only, for the mod menu toggle/options integration; the mod itself ships as its
	// own jar via launcher/mods-bundle, same as Continuity above.
	compileOnly("maven.modrinth:3dskinlayers:1.11.2")

	// Sodium - bundled rather than reimplemented, see https://github.com/CaffeineMC/sodium.
	// Compile-time only, for the "External Mods" mod-menu shortcut into Sodium's own settings
	// screen; the mod itself ships as its own jar via launcher/mods-bundle, same as Continuity
	// and 3D Skin Layers above. Version pinned to exactly the jar in mods-bundle (mc26.1.2-0.9.2).
	compileOnly("maven.modrinth:sodium:mc26.1.2-0.9.2-fabric")
}

tasks.processResources {
	inputs.property("version", project.version)
	inputs.property("minecraft_version", minecraft_version)
	inputs.property("java_version", java_version)

	// filteringCharset wasn't previously set explicitly - JDK 21 already defaults to UTF-8, but
	// fabric.mod.json now contains its first non-ASCII character (the "§" cape-URL token below),
	// so pin this rather than rely on the platform default.
	filteringCharset = "UTF-8"

	filesMatching("fabric.mod.json") {
		expand(mapOf("version" to project.version, "minecraft_version" to minecraft_version, "java_version" to java_version))
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
