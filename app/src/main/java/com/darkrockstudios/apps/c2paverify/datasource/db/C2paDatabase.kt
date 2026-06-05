package com.darkrockstudios.apps.c2paverify.datasource.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.darkrockstudios.apps.c2paverify.datasource.db.dao.UserTrustDao
import com.darkrockstudios.apps.c2paverify.datasource.db.entity.UserTrustRuleEntity

@Database(
	entities = [
		UserTrustRuleEntity::class,
	],
	version = 1,
	exportSchema = true,
)
abstract class C2paDatabase : RoomDatabase() {
	abstract fun userTrustDao(): UserTrustDao
}
