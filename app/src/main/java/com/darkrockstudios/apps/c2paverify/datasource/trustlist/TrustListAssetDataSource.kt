package com.darkrockstudios.apps.c2paverify.datasource.trustlist

import android.content.Context
import com.darkrockstudios.apps.c2paverify.model.trust.TrustMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the trust material bundled in `assets/trust/` (a snapshot of the official C2PA trust
 * list), used for offline first-run and as a fallback when no remote refresh is available.
 */
class TrustListAssetDataSource(private val context: Context) {

	suspend fun loadBundled(): TrustMaterial = withContext(Dispatchers.IO) {
		TrustMaterial(
			trustAnchorsPem = readAsset(ANCHORS_PATH),
			allowedListPem = null,
			trustConfig = null,
		)
	}

	private fun readAsset(path: String): String =
		context.assets.open(path).use { it.readBytes().decodeToString() }

	private companion object {
		const val ANCHORS_PATH = "trust/c2pa-trust-anchors.pem"
	}
}
