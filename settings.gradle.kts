pluginManagement {
	repositories {
		google {
			content {
				includeGroupByRegex("com\\.android.*")
				includeGroupByRegex("com\\.google.*")
				includeGroupByRegex("androidx.*")
			}
		}
		mavenCentral()
		gradlePluginPortal()
	}
}
plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		google()
		mavenCentral()
		// c2pa-android is published via JitPack. Restrict it to that group so it can't shadow
		// other coordinates.
		maven("https://jitpack.io") {
			content { includeGroup("com.github.contentauth") }
		}
	}
}

rootProject.name = "C2PAViewer"
include(":app")
 