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
				aiModified = AiModifiedIndicator(isAiModified = false, sourceTypes = emptyList()),
				capture = CaptureIndicator(isCameraCapture = false, sourceTypes = emptyList()),
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
			aiModified = detectAiModified(manifest),
			capture = detectCapture(manifest),
			revoked = manifest.signerRevoked,
		)
	}

	/**
	 * Flags AI involvement from the **asset's own claim** — i.e. the active manifest's
	 * `digitalSourceType` values that denote algorithmic media (IPTC: `trainedAlgorithmicMedia`,
	 * `compositeWithTrainedAlgorithmicMedia`). Ingredient/provenance history is deliberately *not*
	 * scanned: a normal photo that merely incorporated an AI-generated ingredient is not itself
	 * "AI-generated". That history is still visible in the deep-dive's ingredient list.
	 */
	fun detectAi(manifest: C2paManifestData): AiIndicator {
		val sourceTypes = activeSourceTypes(manifest)
		val isAi = sourceTypes.any { it.segment() in GENERATED_SOURCE_TYPES }
		return AiIndicator(isAiGenerated = isAi, sourceTypes = sourceTypes)
	}

	/**
	 * Flags real content that was modified/composited with AI — IPTC
	 * `compositeWithTrainedAlgorithmicMedia` on the active claim (e.g. a camera photo with a
	 * generative-fill edit). Distinct from [detectAi] (wholly generated). Best-effort: tools that
	 * record only a software-agent name and no `digitalSourceType` won't be detected.
	 */
	fun detectAiModified(manifest: C2paManifestData): AiModifiedIndicator {
		val sourceTypes = activeSourceTypes(manifest)
		val isModified = sourceTypes.any { it.segment() in MODIFIED_SOURCE_TYPES }
		return AiModifiedIndicator(isAiModified = isModified, sourceTypes = sourceTypes)
	}

	/**
	 * Flags a capture-device origin from the asset's own claim — IPTC `digitalCapture` (a camera or
	 * scanner) or `computationalCapture` (computational photography). Scans only the active manifest,
	 * matching [detectAi]'s asset-own-claim scope.
	 */
	fun detectCapture(manifest: C2paManifestData): CaptureIndicator {
		val sourceTypes = activeSourceTypes(manifest)
		val isCapture = sourceTypes.any { it.segment() in CAPTURE_SOURCE_TYPES }
		return CaptureIndicator(isCameraCapture = isCapture, sourceTypes = sourceTypes)
	}

	/** Last path segment of an IPTC digitalSourceType URI, lower-cased. */
	private fun String.segment(): String = substringAfterLast('/').lowercase()

	// Wholly AI-generated (no real source material).
	private val GENERATED_SOURCE_TYPES = setOf("trainedalgorithmicmedia", "algorithmicmedia")

	// Real content modified/composited with AI.
	private val MODIFIED_SOURCE_TYPES = setOf("compositewithtrainedalgorithmicmedia")

	private val CAPTURE_SOURCE_TYPES = setOf("digitalcapture", "computationalcapture")

	private fun activeSourceTypes(manifest: C2paManifestData): List<String> = buildList {
		manifest.activeManifest?.assertions?.forEach { a -> collectDigitalSourceTypes(a.data, this) }
	}.distinct()

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
