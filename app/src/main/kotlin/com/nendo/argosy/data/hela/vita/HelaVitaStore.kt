package com.nendo.argosy.data.hela.vita

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
class HelaVitaStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val autoReleaseZip: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[AUTO_RELEASE_ZIP] ?: false }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    fun setAutoReleaseZip(enabled: Boolean) {
        scope.launch { dataStore.edit { it[AUTO_RELEASE_ZIP] = enabled } }
    }

    private companion object {
        val AUTO_RELEASE_ZIP = booleanPreferencesKey("hela_vita_auto_release_zip")
    }
}
