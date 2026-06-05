package com.darkrockstudios.apps.c2paverify.model.c2pa

import kotlinx.serialization.json.JsonElement

/**
 * Parsed, UI-facing view of a C2PA manifest store. Pure Kotlin (KMP-clean). Built by
 * `C2paManifestParser` from the c2pa-android `reader.json()` output.
 */
data class C2paManifestData(
	val activeManifestId: String?,
	val manifests: List<C2paManifest>,
	val validationState: ManifestValidationState,
	val validationIssues: List<ValidationIssue>,
	/** Raw `reader.json()` payload, kept for the raw-JSON explorer. */
	val rawManifestJson: String,
	/** Raw `reader.detailedJson()` payload, if available. */
	val rawDetailedJson: String? = null,
) {
	val activeManifest: C2paManifest? =
		manifests.firstOrNull { it.id == activeManifestId } ?: manifests.firstOrNull()

	/** Failure-category issues affecting the active manifest's integrity (not mere trust). */
	val integrityFailures: List<ValidationIssue>
		get() = validationIssues.filter { it.category == ValidationCategory.FAILURE && it.isIntegrityFailure }

	/** True when c2pa reported the signer's certificate is not on the configured trust list. */
	val signerUntrusted: Boolean
		get() = validationIssues.any { it.code == CODE_SIGNING_UNTRUSTED }

	/** True when c2pa explicitly reported the signer's certificate as trusted. */
	val signerTrusted: Boolean
		get() = validationIssues.any { it.code == CODE_SIGNING_TRUSTED }

	/**
	 * True when an OCSP response stapled into the signature reported the signer's certificate as
	 * revoked. ([endsWith] avoids matching the `…ocsp.notRevoked` success code.)
	 */
	val signerRevoked: Boolean
		get() = validationIssues.any { it.code.lowercase().endsWith("ocsp.revoked") }

	companion object {
		const val CODE_SIGNING_UNTRUSTED = "signingCredential.untrusted"
		const val CODE_SIGNING_TRUSTED = "signingCredential.trusted"
		const val CODE_SIGNING_REVOKED = "signingCredential.ocsp.revoked"
	}
}

enum class ManifestValidationState { VALID, INVALID, UNKNOWN }

data class C2paManifest(
	val id: String,
	val title: String?,
	val format: String?,
	val claimGenerator: String?,
	val instanceId: String?,
	val signature: SignatureInfo?,
	val assertions: List<C2paAssertion>,
	val ingredients: List<C2paIngredient>,
)

data class SignatureInfo(
	val issuer: String?,
	val commonName: String?,
	val algorithm: String?,
	val certSerialNumber: String?,
	/** ISO-8601 signing time, as reported by c2pa (may be null if unsigned/no timestamp). */
	val time: String?,
)

data class C2paAssertion(
	val label: String,
	val kind: String?,
	/** The assertion's `data` object, retained raw for the deep-dive explorer and AI detection. */
	val data: JsonElement?,
)

data class C2paIngredient(
	val title: String?,
	val format: String?,
	val relationship: String?,
	val instanceId: String?,
	val documentId: String?,
)

enum class ValidationCategory { SUCCESS, INFORMATIONAL, FAILURE, UNKNOWN }

data class ValidationIssue(
	val code: String,
	val explanation: String?,
	val url: String?,
	val category: ValidationCategory,
) {
	/** Codes that indicate the content/signature itself is broken, as opposed to trust. */
	val isIntegrityFailure: Boolean
		get() = INTEGRITY_FAILURE_CODES.any { code.startsWith(it) }

	private companion object {
		val INTEGRITY_FAILURE_CODES = listOf(
			"claimSignature.mismatch",
			"assertion.dataHash.mismatch",
			"assertion.boxesHash.mismatch",
			"assertion.hashedURI.mismatch",
			"claim.missing",
			"claim.hashMismatch",
			"signature", // signature.* hard failures
		)
	}
}
