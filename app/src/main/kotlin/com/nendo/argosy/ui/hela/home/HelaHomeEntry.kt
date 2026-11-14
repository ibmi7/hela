package com.nendo.argosy.ui.hela.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nendo.argosy.data.hela.home.HelaHomeStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HelaHomeEntryPoint {
    fun helaHomeStore(): HelaHomeStore
}

@Composable
internal fun rememberHelaHomeStore(): HelaHomeStore? {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            EntryPointAccessors
                .fromApplication(context.applicationContext, HelaHomeEntryPoint::class.java)
                .helaHomeStore()
        }.getOrNull()
    }
}

/**
 * Whether the Hela home is selected; null while the setting has not been read yet.
 */
@Composable
fun rememberHelaHomeEnabled(): Boolean? {
    val store = rememberHelaHomeStore() ?: return false
    val enabled by store.enabled.collectAsState()
    return enabled
}
