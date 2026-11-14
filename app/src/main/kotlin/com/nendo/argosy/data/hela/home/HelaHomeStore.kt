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
import java.util.concurrent.atomic.AtomicInteger
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

    private val consolesIndex = AtomicInteger(NOT_ON_CONSOLES)

    /**
     * Focused tile of the consoles page while the Hela home is on it, null on a games page.
     * Held in memory for the process, across the home screen being rebuilt and a role swap.
     */
    var consolesPosition: Int?
        get() = consolesIndex.get().takeIf { it != NOT_ON_CONSOLES }
        set(value) = consolesIndex.set(value ?: NOT_ON_CONSOLES)

    private companion object {
        const val NOT_ON_CONSOLES = -1
        val ENABLED = booleanPreferencesKey("hela_home_enabled")
    }
}
