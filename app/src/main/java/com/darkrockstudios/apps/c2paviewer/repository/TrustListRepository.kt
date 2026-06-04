package com.darkrockstudios.apps.c2paviewer.repository

import com.darkrockstudios.apps.c2paviewer.datasource.trustlist.TrustAnchorParser
import com.darkrockstudios.apps.c2paviewer.datasource.trustlist.TrustListAssetDataSource
import com.darkrockstudios.apps.c2paviewer.datasource.trustlist.TrustListCacheDataSource
import com.darkrockstudios.apps.c2paviewer.datasource.trustlist.TrustListRemoteDataSource
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustAnchorInfo
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustMaterial
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Provides the active trust material used to validate signers, plus the parsed list of trusted
 * CAs for display. Prefers a network-refreshed cache (file) and falls back to the bundled asset
 * snapshot; both are parsed for display. In-memory cached and refreshable.
 */
class TrustListRepository(
	private val asset: TrustListAssetDataSource,
	private val cache: TrustListCacheDataSource,
	private val remote: TrustListRemoteDataSource,
	private val parser: TrustAnchorParser,
) {

	private val mutex = Mutex()
	private var cachedMaterial: TrustMaterial? = null

	suspend fun current(): TrustMaterial = mutex.withLock {
		cachedMaterial ?: loadActive().also { cachedMaterial = it }
	}

	private suspend fun loadActive(): TrustMaterial {
		val cachedPem = cache.load()?.takeIf { it.contains("BEGIN CERTIFICATE") }
		return if (cachedPem != null) TrustMaterial(cachedPem) else asset.loadBundled()
	}

	/** Parsed view of the trusted signing CAs in the active trust list. */
	suspend fun anchors(): List<TrustAnchorInfo> = parser.parse(current().trustAnchorsPem)

	/** Epoch millis the trust list was last refreshed from the network, or null if bundled. */
	fun lastUpdatedEpochMs(): Long? = cache.lastUpdatedEpochMs()

	/** Fetches the latest official trust list and replaces the active material on success. */
	suspend fun refresh(): Result<Unit> = mutex.withLock {
		runCatching {
			val pem = remote.fetchCaAnchorsPem()
			require(pem.contains("BEGIN CERTIFICATE")) { "Trust list response had no certificates" }
			cache.save(pem)
			cachedMaterial = TrustMaterial(pem)
		}
	}
}
