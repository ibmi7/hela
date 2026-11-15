package com.nendo.argosy.ui.hela.vita

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nendo.argosy.data.emulator.EmulatorRegistry
import com.nendo.argosy.data.emulator.LaunchConfig
import com.nendo.argosy.data.hela.vita.HelaVitaStore
import com.nendo.argosy.data.platform.PlatformDefinitions
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HelaVitaEntryPoint {
    fun helaVitaStore(): HelaVitaStore
}

@Composable
fun rememberHelaVitaAutoReleaseZip(): Boolean {
    val context = LocalContext.current
    val store = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(context.applicationContext, HelaVitaEntryPoint::class.java)
                .helaVitaStore()
        }.getOrNull()
    } ?: return false
    val enabled by store.autoReleaseZip.collectAsState()
    return enabled
}

fun showsVitaZipRelease(platformSlug: String, emulatorId: String?): Boolean =
    PlatformDefinitions.getCanonicalSlug(platformSlug) == "vita" &&
        emulatorId?.let { EmulatorRegistry.getById(it)?.launchConfig } is LaunchConfig.Vita3K
