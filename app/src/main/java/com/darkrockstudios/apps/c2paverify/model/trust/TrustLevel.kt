package com.darkrockstudios.apps.c2paverify.model.trust

/**
 * Whether the signer of a C2PA manifest is trusted. Derived from c2pa's `signingCredential.*`
 * validation codes (once a trust list is configured, step 4b) combined with the user's
 * allow/deny list.
 */
enum class TrustLevel {
	/** Signer cert chains to a configured trust anchor (or user-allowed). */
	TRUSTED,

	/** Signer is not on any trust list (or user-denied). */
	UNTRUSTED,

	/** Trust could not be determined (e.g. no trust list configured yet). */
	UNKNOWN,
}
