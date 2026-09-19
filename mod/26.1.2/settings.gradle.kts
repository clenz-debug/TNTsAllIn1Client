pluginManagement {
	val loom_version: String by settings

	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		mavenCentral()
		gradlePluginPortal()
	}

	plugins {
		id("net.fabricmc.fabric-loom") version loom_version
	}
}

rootProject.name = "tntsallin1client"
