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

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:$minecraft_version")
	// Mojang's own names, as in the other Fabric versions - 1.14.4 is the first version they exist for.
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:$loader_version")

	modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_api_version")
}

tasks.processResources {
	inputs.property("version", project.version)
	inputs.property("minecraft_version", minecraft_version)
	inputs.property("java_version", java_version)

	filteringCharset = "UTF-8"

	filesMatching("fabric.mod.json") {
		expand(mapOf("version" to project.version, "minecraft_version" to minecraft_version, "java_version" to java_version))
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.release.set(java_version.toInt())
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	withSourcesJar()
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
