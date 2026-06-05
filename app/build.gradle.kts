plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.ksp)
}

android {
	namespace = "com.darkrockstudios.apps.c2paverify"
	compileSdk {
		version = release(36) {
			minorApiLevel = 1
		}
	}

	defaultConfig {
		applicationId = "com.darkrockstudios.apps.c2paverify"
		minSdk = 29
		targetSdk = 36
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			optimization {
				enable = false
			}
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	buildFeatures {
		compose = true
	}
	packaging {
		resources {
			excludes += setOf(
				"/META-INF/{AL2.0,LGPL2.1}",
				"META-INF/LICENSE.md",
				"META-INF/LICENSE-notice.md",
				"META-INF/LICENSE",
				"META-INF/LICENSE.txt",
				"META-INF/NOTICE.md",
				"META-INF/NOTICE",
				"META-INF/NOTICE.txt",
			)
		}
	}
}

kotlin {
	compilerOptions {
		jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
	}
}

ksp {
	arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
	// Compose / AndroidX UI
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.compose.material3)
	implementation(libs.androidx.compose.material.icons.core)
	implementation(libs.androidx.compose.ui)
	implementation(libs.androidx.compose.ui.graphics)
	implementation(libs.androidx.compose.ui.tooling.preview)
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.lifecycle.runtime.compose)
	implementation(libs.androidx.lifecycle.viewmodel.compose)

	// Adaptive multi-pane + navigation
	implementation(libs.androidx.adaptive)
	implementation(libs.androidx.adaptive.layout)
	implementation(libs.androidx.adaptive.navigation)
	implementation(libs.androidx.navigation.compose)

	// DI (Koin)
	implementation(platform(libs.koin.bom))
	implementation(libs.koin.android)
	implementation(libs.koin.androidx.compose)

	// Coroutines & serialization
	implementation(libs.kotlinx.coroutines.android)
	implementation(libs.kotlinx.serialization.json)

	// HTTP (Ktor)
	implementation(libs.ktor.client.core)
	implementation(libs.ktor.client.okhttp)
	implementation(libs.ktor.client.content.negotiation)
	implementation(libs.ktor.serialization.kotlinx.json)

	// Image loading & zoom
	implementation(libs.coil.compose)
	implementation(libs.coil.network.ktor3)
	implementation(libs.telephoto.zoomable.image.coil3)

	// C2PA reading/verification (Android-only native lib via JitPack; needs JNA aar).
	// c2pa-android transitively pulls the plain jna *jar*; on Android we need the *aar* (which
	// carries the native .so libs), so exclude the transitive jar to avoid duplicate classes.
	implementation(libs.c2pa.android) {
		exclude(group = "net.java.dev.jna", module = "jna")
	}
	implementation("net.java.dev.jna:jna:${libs.versions.jna.get()}@aar")

	// Storage & logging
	implementation(libs.androidx.room.runtime)
	implementation(libs.androidx.room.ktx)
	ksp(libs.androidx.room.compiler)
	implementation(libs.androidx.datastore.preferences)
	implementation(libs.napier)

	// Unit tests
	testImplementation(libs.junit)
	testImplementation(libs.kotlinx.coroutines.test)
	testImplementation(libs.turbine)
	testImplementation(libs.mockk)
	testImplementation(libs.koin.test)
	testImplementation(libs.koin.test.junit4)
	testImplementation(libs.konsist)
	testImplementation(libs.androidx.room.testing)

	// Instrumented / E2E tests
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.compose.ui.test.junit4)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.mockk.android)
	debugImplementation(libs.androidx.compose.ui.test.manifest)
	debugImplementation(libs.androidx.compose.ui.tooling)
}