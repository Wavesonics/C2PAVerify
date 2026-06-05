package com.darkrockstudios.apps.c2paverify.datasource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined allow/deny rule for a signing authority, keyed by the issuing CA's stable
 * SHA-256 thumbprint (not the spoofable subject DN, which is kept only for display).
 */
@Entity(tableName = "user_trust_rule")
data class UserTrustRuleEntity(
	@PrimaryKey val authorityKey: String,
	val displayName: String,
	val rule: String, // ALLOW | DENY
	val createdAt: Long,
	val note: String? = null,
)
