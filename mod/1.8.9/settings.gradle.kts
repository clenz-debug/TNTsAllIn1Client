plugins {
	// Lets Gradle download the Java 8 runtime `verifyMixins` starts the game's class loading with -
	// 1.8.9 (and LaunchWrapper) don't run on anything newer.
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "tntsallin1client"
