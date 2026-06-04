package com.darkrockstudios.apps.c2paviewer.datasource.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Lightweight scalar app preferences backed by Preferences DataStore (Room would be overkill).
 * Android-specific; keeps `android.*`/DataStore wiring out of the KMP-clean layers above.
 */
class PreferencesDataSource(private val context: Context) {

	/** Whether the user has finished (or skipped) the intro slideshow. Defaults to false. */
	val onboardingSeen: Flow<Boolean> =
		context.settingsDataStore.data.map { it[ONBOARDING_SEEN] ?: false }

	suspend fun setOnboardingSeen(seen: Boolean) {
		context.settingsDataStore.edit { it[ONBOARDING_SEEN] = seen }
	}

	private companion object {
		val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
	}
}
