package com.darkrockstudios.apps.c2paviewer.model.summary

import com.darkrockstudios.apps.c2paviewer.model.c2pa.C2paManifestData
import com.darkrockstudios.apps.c2paviewer.model.c2pa.ManifestValidationState
import com.darkrockstudios.apps.c2paviewer.model.trust.TrustLevel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Pure domain logic that turns a parsed manifest + trust verdict into a [C2paSummary]. Lives in
 * `model/` (not a use case) because it is non-I/O and trivially unit-testable.
 */
object SummaryFactory {

	fun buildSummary(manifest: C2paManifestData?, trust: TrustLevel): C2paSummary {
		if (manifest == null) {
			return C2paSummary(
				status = OverallStatus.NO_MANIFEST,
				signerName = null,
				signerCommonName = null,
				claimGenerator = null,
				signedTime = null,
				ai = AiIndicator(isAiGenerated = false, sourceTypes = emptyList()),
			)
		}

		val active = manifest.activeManifest
		val status = when {
			manifest.validationState == ManifestValidationState.INVALID ||
				manifest.integrityFailures.isNotEmpty() -> OverallStatus.TAMPERED_INVALID

			trust == TrustLevel.TRUSTED -> OverallStatus.SIGNED_TRUSTED
			else -> OverallStatus.SIGNED_UNTRUSTED
		}

		return C2paSummary(
			status = status,
			signerName = active?.signature?.issuer,
			signerCommonName = active?.signature?.commonName,
			claimGenerator = active?.claimGenerator,
			signedTime = active?.signature?.time,
			ai = detectAi(manifest),
		)
	}

	/**
	 * Flags AI involvement by scanning every assertion's data for `digitalSourceType` values that
	 * denote algorithmic media (IPTC: `trainedAlgorithmicMedia`, `compositeWithTrainedAlgorithmicMedia`).
	 */
	fun detectAi(manifest: C2paManifestData): AiIndicator {
		val sourceTypes = buildList {
			manifest.manifests.forEach { m ->
				m.assertions.forEach { a -> collectDigitalSourceTypes(a.data, this) }
			}
		}.distinct()
		val isAi = sourceTypes.any { it.lowercase().contains("algorithmicmedia") }
		return AiIndicator(isAiGenerated = isAi, sourceTypes = sourceTypes)
	}

	private fun collectDigitalSourceTypes(element: JsonElement?, out: MutableList<String>) {
		when (element) {
			is JsonObject -> element.forEach { (key, value) ->
				if (key == "digitalSourceType" && value is JsonPrimitive && value.isString) {
					out.add(value.content)
				} else {
					collectDigitalSourceTypes(value, out)
				}
			}

			is JsonArray -> element.forEach { collectDigitalSourceTypes(it, out) }
			else -> Unit
		}
	}
}
