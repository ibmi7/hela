package com.nendo.argosy.data.hela.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HelaHomeStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Whether the Hela home replaces Argosy's on the home route; null until DataStore has
     * been read once.
     */
    val enabled: StateFlow<Boolean?> = dataStore.data
        .map { prefs -> prefs[ENABLED] ?: false }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    fun setEnabled(enabled: Boolean) {
        scope.launch { dataStore.edit { it[ENABLED] = enabled } }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("hela_home_enabled")
    }
}
