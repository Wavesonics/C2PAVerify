package com.darkrockstudios.apps.c2paviewer.model.summary

import com.darkrockstudios.apps.c2paviewer.model.c2pa.C2paManifestData

/** High-level verdict shown on the summary card. */
enum class OverallStatus {
	/** No embedded C2PA manifest. */
	NO_MANIFEST,

	/** Manifest valid and signer trusted. */
	SIGNED_TRUSTED,

	/** Manifest valid but signer not on a trust list. */
	SIGNED_UNTRUSTED,

	/** Manifest present but failed integrity/signature validation. */
	TAMPERED_INVALID,
}

/** Whether the asset declares AI involvement, derived from `digitalSourceType` assertions. */
data class AiIndicator(
	val isAiGenerated: Boolean,
	/** The raw `digitalSourceType` values found (IPTC URIs), for display/debugging. */
	val sourceTypes: List<String>,
)

/**
 * Whether the asset's own claim says real content was modified/composited with AI — IPTC
 * `compositeWithTrainedAlgorithmicMedia` — as opposed to being wholly AI-generated ([AiIndicator])
 * or untouched.
 */
data class AiModifiedIndicator(
	val isAiModified: Boolean,
	val sourceTypes: List<String>,
)

/**
 * Whether the asset's own claim says it was captured by a device (camera/scanner) — IPTC
 * `digitalCapture` or `computationalCapture` — as opposed to being created or edited in software.
 */
data class CaptureIndicator(
	val isCameraCapture: Boolean,
	val sourceTypes: List<String>,
)

/** The up-front summary of an inspection. */
data class C2paSummary(
	val status: OverallStatus,
	val signerName: String?,
	val signerCommonName: String?,
	val claimGenerator: String?,
	val signedTime: String?,
	val ai: AiIndicator,
	val aiModified: AiModifiedIndicator = AiModifiedIndicator(isAiModified = false, sourceTypes = emptyList()),
	val capture: CaptureIndicator = CaptureIndicator(isCameraCapture = false, sourceTypes = emptyList()),
	/** The signer's certificate was reported revoked (via a stapled OCSP response). */
	val revoked: Boolean = false,
) {
	val manifestPresent: Boolean get() = status != OverallStatus.NO_MANIFEST
}

/** Result of inspecting a photo: the headline [summary] plus the full [manifest] (if any). */
data class InspectionResult(
	val summary: C2paSummary,
	val manifest: C2paManifestData?,
	/** Whether the user has an explicit allow/deny rule for this signer. */
	val signerHasOverride: Boolean = false,
)
