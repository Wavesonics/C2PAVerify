package com.darkrockstudios.apps.c2paviewer.model.trust

/** A human-readable view of one trusted signing CA from the trust list. */
data class TrustAnchorInfo(
	val displayName: String,
	val subject: String,
	val notBeforeEpochMs: Long?,
	val notAfterEpochMs: Long?,
)
