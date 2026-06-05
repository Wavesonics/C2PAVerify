package com.darkrockstudios.apps.c2paverify.model.trust

/**
 * The trust material handed to the C2PA reader to validate a signer against a trust list.
 * Pure Kotlin (KMP-clean); sourced from the bundled asset and (later) a remote refresh.
 *
 * @property trustAnchorsPem PEM bundle of trusted signing CA roots/intermediates.
 * @property allowedListPem optional PEM of explicitly-allowed end-entity certificates.
 * @property trustConfig optional EKU "store config" (allowed extended-key-usage OIDs).
 */
data class TrustMaterial(
	val trustAnchorsPem: String,
	val allowedListPem: String? = null,
	val trustConfig: String? = null,
) {
	val hasAnchors: Boolean get() = trustAnchorsPem.contains("BEGIN CERTIFICATE")
}
