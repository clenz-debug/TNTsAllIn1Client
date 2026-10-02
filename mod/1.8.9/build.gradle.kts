import java.nio.file.Files
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import net.fabricmc.tinyremapper.TinyUtils
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension

// Our mod for a legacy Minecraft (before 1.14, no Fabric): not loaded by a mod loader, but through
// an entry of our own. Three parts:
//  - the entry: a LaunchWrapper tweaker (`launch/ClientTweaker`) the launcher starts the game through,
//  - the hook: Mixin, same technique as in the Fabric mods,
//  - the names: 1.8.9 is obfuscated (the title screen is `aya`). Legacy Yarn names the game; the
//    game jar is translated to those names to compile against, and the built mod is translated
//    back to the obfuscated names the real game has at runtime.

buildscript {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		mavenCentral()
	}
	dependencies {
		classpath("net.fabricmc:tiny-remapper:${project.property("tiny_remapper_version")}")
	}
}

plugins {
	java
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
	archivesName.set("tntsallin1client")
}

repositories {
	// Mojang's client jar, addressed by its hash - not redistributed, every build downloads it.
	ivy {
		name = "MojangClientJar"
		url = uri("https://piston-data.mojang.com/v1/objects/")
		patternLayout { artifact("[revision]/[artifact].[ext]") }
		metadataSources { artifact() }
		content { includeGroup("com.mojang.minecraft") }
	}
	maven {
		name = "MojangLibraries"
		url = uri("https://libraries.minecraft.net/")
		content { includeGroup("net.minecraft") }
	}
	maven {
		name = "LegacyFabric"
		url = uri("https://maven.legacyfabric.net/")
		content { includeGroup("net.legacyfabric") }
	}
	maven {
		name = "SpongePowered"
		url = uri("https://repo.spongepowered.org/repository/maven-public/")
		content { includeGroup("org.spongepowered") }
	}
	mavenCentral()
}

/** The obfuscated client jar exactly as Mojang ships it. */
val minecraftObfuscated: Configuration by configurations.creating
/** Legacy Yarn's jar holding `mappings/mappings.tiny` (obfuscated, intermediary and readable names). */
val mappingsJar: Configuration by configurations.creating
/** Libraries that end up inside our jar - the launcher puts just that one jar on the classpath. */
val shade: Configuration by configurations.creating
/** What `verifyMixins` needs next to our jar and the game: the entry's libraries (see the launcher's
 * `legacyClientEntry.ts`) and the few game libraries LaunchWrapper and Mixin use themselves. */
val verifyRuntime: Configuration by configurations.creating

configurations.compileOnly.get().extendsFrom(shade)

/** Translates a jar between two name sets of a tiny v2 mapping file. */
abstract class RemapJar : DefaultTask() {
	@get:InputFile
	abstract val input: RegularFileProperty

	@get:InputFile
	abstract val mappings: RegularFileProperty

	@get:Input
	abstract val fromNamespace: Property<String>

	@get:Input
	abstract val toNamespace: Property<String>

	/** Also translate the names inside Mixin annotations (`@Inject(method = "render")`, `@Shadow`
	 * members, ...) - text the plain translation doesn't touch. For our own jar only. */
	@get:Input
	abstract val remapMixinAnnotations: Property<Boolean>

	/** Jars the input's classes inherit from - needed to rename inherited members correctly. */
	@get:Classpath
	abstract val classpath: ConfigurableFileCollection

	@get:OutputFile
	abstract val output: RegularFileProperty

	init {
		remapMixinAnnotations.convention(false)
	}

	@TaskAction
	fun remap() {
		val inputPath = input.get().asFile.toPath()
		val outputPath = output.get().asFile.toPath()
		Files.deleteIfExists(outputPath)
		val builder = TinyRemapper.newRemapper()
			.withMappings(TinyUtils.createTinyMappingProvider(mappings.get().asFile.toPath(), fromNamespace.get(), toNamespace.get()))
		if (remapMixinAnnotations.get()) {
			builder.extension(MixinExtension())
		}
		val remapper = builder.build()
		try {
			OutputConsumerPath.Builder(outputPath).build().use { consumer ->
				consumer.addNonClassFiles(inputPath)
				remapper.readInputs(inputPath)
				remapper.readClassPath(*classpath.files.map { it.toPath() }.toTypedArray())
				remapper.apply(consumer)
			}
		} finally {
			remapper.finish()
		}
	}
}

val extractMappings by tasks.registering(Copy::class) {
	description = "Unpacks Legacy Yarn's mapping file."
	from(mappingsJar.elements.map { jars -> jars.map { zipTree(it) } }) {
		include("mappings/mappings.tiny")
	}
	into(layout.buildDirectory.dir("mappings"))
	eachFile { path = name }
	includeEmptyDirs = false
}
val mappingsFile = extractMappings.map { layout.buildDirectory.file("mappings/mappings.tiny").get() }

val remapMinecraft by tasks.registering(RemapJar::class) {
	description = "Translates the obfuscated client jar to Legacy Yarn's names."
	input.fileProvider(provider { minecraftObfuscated.singleFile })
	mappings.set(mappingsFile)
	fromNamespace.set("official")
	toNamespace.set("named")
	output.set(layout.buildDirectory.file("minecraft/minecraft-${project.property("minecraft_version")}-named.jar"))
}

dependencies {
	minecraftObfuscated("com.mojang.minecraft:client:${project.property("minecraft_client_sha1")}@jar")
	mappingsJar("net.legacyfabric:yarn:${project.property("legacy_yarn_version")}:mergedv2@jar")
	compileOnly(files(remapMinecraft.flatMap { it.output }))

	// On the classpath at runtime through the launcher, see `legacyClientEntry.ts`.
	compileOnly("net.minecraft:launchwrapper:${project.property("launchwrapper_version")}") { isTransitive = false }
	shade("org.spongepowered:mixin:${project.property("mixin_version")}") { isTransitive = false }
	// The game brings these itself.
	compileOnly("com.google.code.gson:gson:2.2.4")
	compileOnly("com.google.guava:guava:17.0")
	compileOnly("org.apache.logging.log4j:log4j-api:2.0-beta9")
	// On the classpath through the launcher (for Mixin) - `VerifyMain` uses it.
	compileOnly("org.ow2.asm:asm-all:5.0.3")

	verifyRuntime("net.minecraft:launchwrapper:${project.property("launchwrapper_version")}") { isTransitive = false }
	verifyRuntime("org.ow2.asm:asm-all:5.0.3")
	verifyRuntime("net.sf.jopt-simple:jopt-simple:4.6")
	verifyRuntime("org.apache.logging.log4j:log4j-api:2.0-beta9")
	verifyRuntime("org.apache.logging.log4j:log4j-core:2.0-beta9")
	verifyRuntime("com.google.guava:guava:17.0")
	verifyRuntime("com.google.code.gson:gson:2.2.4")
	verifyRuntime("commons-io:commons-io:2.4")
	verifyRuntime("org.apache.commons:commons-lang3:3.3.2")
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set((project.property("java_version") as String).toInt())
	options.encoding = "UTF-8"
	// Java 8 as a target is deprecated in current JDKs - known, and what this game version needs.
	options.compilerArgs.add("-Xlint:-options")
}

// The compiled mod, still using the readable names - can't run against the real game.
tasks.jar {
	archiveClassifier.set("dev")
}

val reobfJar by tasks.registering(RemapJar::class) {
	description = "Translates the built mod back to the obfuscated names of the real game."
	input.set(tasks.jar.flatMap { it.archiveFile })
	mappings.set(mappingsFile)
	fromNamespace.set("named")
	toNamespace.set("official")
	remapMixinAnnotations.set(true)
	classpath.from(remapMinecraft.flatMap { it.output })
	output.set(layout.buildDirectory.file("reobf/tntsallin1client-reobf.jar"))
}

// What the launcher starts the game with: the translated mod plus the shaded libraries.
val clientJar by tasks.registering(Jar::class) {
	from(reobfJar.flatMap { it.output }.map { zipTree(it) })
	from(shade.elements.map { jars -> jars.map { zipTree(it) } }) {
		exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "META-INF/MANIFEST.MF")
	}
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.assemble {
	dependsOn(clientJar)
}

// Starts the built jar the way the launcher does - through LaunchWrapper and our tweaker, against
// the real obfuscated game - but with `VerifyMain` in place of the game: every mixin's target class
// is loaded and checked, no game window. Part of `gradlew build`.
val verifyMixins by tasks.registering(JavaExec::class) {
	group = "verification"
	description = "Checks every mixin against the real game without starting it."
	javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(8)) })
	classpath(clientJar, verifyRuntime, minecraftObfuscated)
	mainClass.set("net.minecraft.launchwrapper.Launch")
	args("--tweakClass", "com.tntsallin1client.launch.ClientTweaker", "--version", "verify", "--gameDir", temporaryDir.path, "--assetsDir", temporaryDir.path)
	systemProperty("tntsallin1client.verify", "true")
	systemProperty("log4j.configurationFile", layout.projectDirectory.file("src/verify/log4j2.xml").asFile.toURI().toString())
	inputs.file(layout.projectDirectory.file("src/verify/log4j2.xml"))
	workingDir = temporaryDir
}

tasks.check {
	dependsOn(verifyMixins)
}
