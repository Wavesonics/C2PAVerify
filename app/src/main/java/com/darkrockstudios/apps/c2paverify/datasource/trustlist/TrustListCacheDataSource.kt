package com.darkrockstudios.apps.c2paverify.datasource.trustlist

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Persists the most recently fetched trust-list PEM to the app's files dir. The file's
 * last-modified time doubles as the "last updated" timestamp.
 */
class TrustListCacheDataSource(private val context: Context) {

	private val file: File get() = File(context.filesDir, FILE_NAME)

	suspend fun load(): String? = withContext(Dispatchers.IO) {
		file.takeIf { it.exists() }?.readText()
	}

	suspend fun save(pem: String) = withContext(Dispatchers.IO) {
		file.writeText(pem)
	}

	fun lastUpdatedEpochMs(): Long? = file.takeIf { it.exists() }?.lastModified()

	private companion object {
		const val FILE_NAME = "c2pa-trust-anchors-remote.pem"
	}
}
