package com.darkrockstudios.apps.c2paviewer.repository

import com.darkrockstudios.apps.c2paviewer.datasource.trustlist.TrustListAssetDataSource
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustMaterial
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Provides the active trust material used to validate signers. For now it serves the bundled
 * snapshot (cached in memory); a remote refresh (Ktor) + Room cache is layered on with the trust
 * management UI (step 7).
 */
class TrustListRepository(private val asset: TrustListAssetDataSource) {

	private val mutex = Mutex()
	private var cached: TrustMaterial? = null

	suspend fun current(): TrustMaterial = mutex.withLock {
		cached ?: asset.loadBundled().also { cached = it }
	}
}
