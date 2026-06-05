package com.darkrockstudios.apps.c2paverify.repository

import com.darkrockstudios.apps.c2paverify.datasource.db.dao.UserTrustDao
import com.darkrockstudios.apps.c2paverify.datasource.db.entity.UserTrustRuleEntity
import com.darkrockstudios.apps.c2paverify.model.trust.TrustRule
import com.darkrockstudios.apps.c2paverify.model.trust.UserTrustRule
import com.darkrockstudios.apps.c2paverify.model.trust.anchorSubjectOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * User-managed allow/deny rules for signing authorities, persisted in Room. Keeps `model/`
 * Room-free by mapping entities here.
 */
class UserTrustRepository(private val dao: UserTrustDao) {

	fun observeRules(): Flow<List<UserTrustRule>> =
		dao.observeRules().map { rows -> rows.map { it.toDomain() } }

	suspend fun ruleFor(authorityKey: String): UserTrustRule? =
		dao.observeRule(authorityKey).first()?.toDomain()

	/** CA subjects the user has dis-allowed (CA-level DENY rules). */
	suspend fun blockedAnchorSubjects(): Set<String> =
		observeRules().first()
			.filter { it.rule == TrustRule.DENY }
			.mapNotNull { anchorSubjectOf(it.authorityKey) }
			.toSet()

	suspend fun setRule(rule: UserTrustRule) = dao.upsert(rule.toEntity())

	suspend fun clear(authorityKey: String) = dao.delete(authorityKey)

	private fun UserTrustRuleEntity.toDomain() = UserTrustRule(
		authorityKey = authorityKey,
		displayName = displayName,
		rule = TrustRule.valueOf(rule),
		createdAt = createdAt,
		note = note,
	)

	private fun UserTrustRule.toEntity() = UserTrustRuleEntity(
		authorityKey = authorityKey,
		displayName = displayName,
		rule = rule.name,
		createdAt = createdAt,
		note = note,
	)
}
