package com.darkrockstudios.apps.c2paviewer.repository

import com.darkrockstudios.apps.c2paviewer.datasource.settings.PreferencesDataSource
import kotlinx.coroutines.flow.Flow

/**
 * App settings as observable state. Thin seam over [PreferencesDataSource]; stays KMP-clean
 * (DataStore lives in the data layer).
 */
class SettingsRepository(private val prefs: PreferencesDataSource) {
	val onboardingSeen: Flow<Boolean> = prefs.onboardingSeen

	suspend fun markOnboardingSeen() = prefs.setOnboardingSeen(true)
}
