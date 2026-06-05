package com.darkrockstudios.apps.c2paverify.datasource.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.darkrockstudios.apps.c2paverify.datasource.db.entity.UserTrustRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserTrustDao {
	@Query("SELECT * FROM user_trust_rule ORDER BY displayName")
	fun observeRules(): Flow<List<UserTrustRuleEntity>>

	@Query("SELECT * FROM user_trust_rule WHERE authorityKey = :authorityKey LIMIT 1")
	fun observeRule(authorityKey: String): Flow<UserTrustRuleEntity?>

	@Upsert
	suspend fun upsert(rule: UserTrustRuleEntity)

	@Query("DELETE FROM user_trust_rule WHERE authorityKey = :authorityKey")
	suspend fun delete(authorityKey: String)
}
