package com.darkrockstudios.apps.c2paviewer.repository

import com.darkrockstudios.apps.c2paviewer.datasource.trustlist.TrustAnchorParser
import com.darkrockstudios.apps.c2paviewer.datasource.trustlist.TrustListAssetDataSource
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustAnchorInfo
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustMaterial
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Provides the active trust material used to validate signers, and the parsed list of trusted CAs
 * for display. Serves the bundled snapshot (cached in memory); a remote refresh (Ktor) + cache is
 * layered on in step 7b.
 */
class TrustListRepository(
	private val asset: TrustListAssetDataSource,
	private val parser: TrustAnchorParser,
) {

	private val mutex = Mutex()
	private var cached: TrustMaterial? = null

	suspend fun current(): TrustMaterial = mutex.withLock {
		cached ?: asset.loadBundled().also { cached = it }
	}

	/** Parsed view of the trusted signing CAs in the active trust list. */
	suspend fun anchors(): List<TrustAnchorInfo> = parser.parse(current().trustAnchorsPem)
}
