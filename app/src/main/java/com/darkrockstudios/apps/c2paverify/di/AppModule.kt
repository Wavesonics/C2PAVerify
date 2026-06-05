package com.darkrockstudios.apps.c2paverify.di

import androidx.room.Room
import com.darkrockstudios.apps.c2paverify.datasource.db.C2paDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Platform singletons shared across layers: the Room database (and its DAOs), the JSON parser,
 * and (later) the Ktor client and Coil image loader.
 *
 * The C2PA manifest schema is large and evolving, so [Json] is lenient and ignores unknown keys
 * for forward-compatibility.
 */
val appModule = module {
	single {
		Json {
			ignoreUnknownKeys = true
			isLenient = true
		}
	}

	single {
		Room.databaseBuilder(
			androidContext(),
			C2paDatabase::class.java,
			"c2pa.db",
		).build()
	}
	single { get<C2paDatabase>().userTrustDao() }

	single<HttpClient> { HttpClient(OkHttp) }
}

/**
 * All Koin modules, ordered by layer. As components are added, register them in the matching
 * per-layer module (stateless = factory, stateful = single).
 */
val appModules = listOf(
	appModule,
	dataSourceModule,
	repositoryModule,
	serviceModule,
	useCaseModule,
	viewModelModule,
)
