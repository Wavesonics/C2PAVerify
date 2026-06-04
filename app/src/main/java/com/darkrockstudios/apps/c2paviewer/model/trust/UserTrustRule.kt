package com.darkrockstudios.apps.c2paviewer.model.trust

/** A user-defined override for a signing authority. */
enum class TrustRule { ALLOW, DENY }

data class UserTrustRule(
	val authorityKey: String,
	val displayName: String,
	val rule: TrustRule,
	val createdAt: Long,
	val note: String? = null,
)

/**
 * Stable key identifying a signing authority for user allow/deny rules. The c2pa-android 0.0.9
 * Reader does not expose the raw cert chain/thumbprint, so we key on the available
 * `signature_info` fields (issuer DN + certificate serial number).
 */
fun authorityKeyOf(issuer: String?, certSerialNumber: String?): String? {
	if (issuer.isNullOrBlank() && certSerialNumber.isNullOrBlank()) return null
	return "${issuer.orEmpty()}|${certSerialNumber.orEmpty()}"
}
