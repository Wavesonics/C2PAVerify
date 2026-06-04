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

private const val ANCHOR_KEY_PREFIX = "anchor:"

/**
 * Rule key for dis-allowing a whole trust-list CA (by its certificate subject). Distinct from a
 * per-signer [authorityKeyOf] key: a CA-level DENY removes that anchor from the trust material, so
 * c2pa reports anything chaining to it as untrusted.
 */
fun anchorAuthorityKey(subject: String): String = "$ANCHOR_KEY_PREFIX$subject"

/** The CA subject a CA-level rule targets, or null if [key] is a per-signer key. */
fun anchorSubjectOf(key: String): String? =
	if (key.startsWith(ANCHOR_KEY_PREFIX)) key.removePrefix(ANCHOR_KEY_PREFIX) else null
