package com.nendo.argosy.data.hela.theme

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nendo.argosy.data.model.hela.HelaThemeSelection
import com.nendo.argosy.data.model.hela.HelaWallpapers
import com.nendo.argosy.data.model.hela.InstalledThemePack
import com.nendo.argosy.data.model.hela.PackColors
import com.nendo.argosy.data.model.hela.PackWallpaper
import com.nendo.argosy.data.model.hela.WallpaperKind
import com.nendo.argosy.data.model.hela.WallpaperSlot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HelaThemeStore @Inject constructor(
    @ApplicationContext context: Context,
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val MAIN_PACK = stringPreferencesKey("hela_wallpaper_main_pack")
        val EXTERNAL_PACK = stringPreferencesKey("hela_wallpaper_external_pack")
        val COLORS_PACK = stringPreferencesKey("hela_colors_pack")
        val ANIMATE = booleanPreferencesKey("hela_wallpaper_animate")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val themesDir = File(context.filesDir, "hela/themes")

    private val _packs = MutableStateFlow<List<InstalledThemePack>>(emptyList())
    val packs: StateFlow<List<InstalledThemePack>> = _packs.asStateFlow()

    val selection: Flow<HelaThemeSelection> = dataStore.data
        .map { prefs ->
            HelaThemeSelection(
                mainPackId = prefs[Keys.MAIN_PACK],
                externalPackId = prefs[Keys.EXTERNAL_PACK],
                colorsPackId = prefs[Keys.COLORS_PACK],
                animate = prefs[Keys.ANIMATE] ?: true
            )
        }
        .distinctUntilChanged()

    val wallpapers: StateFlow<HelaWallpapers> = combine(_packs, selection) { packs, selection ->
        HelaWallpapers(
            main = packs.wallpaperOf(selection, WallpaperSlot.MAIN),
            external = packs.wallpaperOf(selection, WallpaperSlot.EXTERNAL),
            animate = selection.animate
        )
    }.stateIn(scope, SharingStarted.Eagerly, HelaWallpapers())

    val colors: StateFlow<PackColors?> = combine(_packs, selection) { packs, selection ->
        packs.firstOrNull { it.id == selection.colorsPackId }?.colors
    }.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        scope.launch { refresh() }
    }

    suspend fun importPack(zipPath: String): PackImportResult = withContext(Dispatchers.IO) {
        themesDir.mkdirs()
        val result = CocoonPackImporter.import(File(zipPath), themesDir)
        if (result is PackImportResult.Success) {
            extractStills(result.packDir)
            refresh()
        }
        result
    }

    suspend fun applyPack(packId: String) {
        val pack = _packs.value.firstOrNull { it.id == packId } ?: return
        dataStore.edit { prefs ->
            if (pack.main != null) prefs[Keys.MAIN_PACK] = pack.id
            if (pack.external != null) prefs[Keys.EXTERNAL_PACK] = pack.id
            if (pack.colors != null) prefs[Keys.COLORS_PACK] = pack.id
        }
    }

    suspend fun setColorsPack(packId: String?) {
        dataStore.edit { prefs ->
            if (packId != null) prefs[Keys.COLORS_PACK] = packId else prefs.remove(Keys.COLORS_PACK)
        }
    }

    suspend fun setSlot(slot: WallpaperSlot, packId: String?) {
        val key = keyOf(slot)
        dataStore.edit { prefs ->
            if (packId != null) prefs[key] = packId else prefs.remove(key)
        }
    }

    suspend fun clearSelection() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.MAIN_PACK)
            prefs.remove(Keys.EXTERNAL_PACK)
            prefs.remove(Keys.COLORS_PACK)
        }
    }

    suspend fun setAnimate(animate: Boolean) {
        dataStore.edit { it[Keys.ANIMATE] = animate }
    }

    suspend fun deletePack(packId: String) = withContext(Dispatchers.IO) {
        dataStore.edit { prefs ->
            if (prefs[Keys.MAIN_PACK] == packId) prefs.remove(Keys.MAIN_PACK)
            if (prefs[Keys.EXTERNAL_PACK] == packId) prefs.remove(Keys.EXTERNAL_PACK)
            if (prefs[Keys.COLORS_PACK] == packId) prefs.remove(Keys.COLORS_PACK)
        }
        File(themesDir, packId).takeIf { it.parentFile == themesDir }?.deleteRecursively()
        refresh()
    }

    private fun keyOf(slot: WallpaperSlot): Preferences.Key<String> = when (slot) {
        WallpaperSlot.MAIN -> Keys.MAIN_PACK
        WallpaperSlot.EXTERNAL -> Keys.EXTERNAL_PACK
    }

    private fun List<InstalledThemePack>.wallpaperOf(
        selection: HelaThemeSelection,
        slot: WallpaperSlot
    ): PackWallpaper? = firstOrNull { it.id == selection.packId(slot) }?.wallpaper(slot)

    private fun refresh() {
        val dirs = themesDir.listFiles { file -> file.isDirectory && !file.name.startsWith(".") }
        _packs.value = dirs.orEmpty().map(::readPack).sortedBy { it.name.lowercase() }
    }

    private fun readPack(packDir: File): InstalledThemePack {
        val manifestFile = File(packDir, CocoonPackImporter.THEME_FILE)
        val json = manifestFile.takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }
        val manifest = CocoonThemeParser.parse(json, fallbackName = packDir.name)
        return InstalledThemePack(
            id = packDir.name,
            name = manifest.name,
            author = manifest.author,
            version = manifest.version,
            main = readWallpaper(packDir, manifest, WallpaperSlot.MAIN),
            external = readWallpaper(packDir, manifest, WallpaperSlot.EXTERNAL),
            colors = CocoonColors.packColors(manifest)
        )
    }

    private fun readWallpaper(
        packDir: File,
        manifest: com.nendo.argosy.data.model.hela.CocoonThemeManifest,
        slot: WallpaperSlot
    ): PackWallpaper? {
        val file = CocoonWallpaperResolver.resolve(packDir, manifest, slot) ?: return null
        val kind = CocoonWallpaperResolver.kindOf(file) ?: return null
        return PackWallpaper(
            path = file.absolutePath,
            kind = kind,
            stillPath = stillFile(packDir, slot).takeIf { it.isFile }?.absolutePath
        )
    }

    private fun stillFile(packDir: File, slot: WallpaperSlot): File =
        File(packDir, "$STILLS_DIR/${slot.token}.jpg")

    private fun extractStills(packDir: File) {
        val manifestFile = File(packDir, CocoonPackImporter.THEME_FILE)
        val json = manifestFile.takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }
        val manifest = CocoonThemeParser.parse(json, fallbackName = packDir.name)
        for (slot in WallpaperSlot.entries) {
            val video = CocoonWallpaperResolver.resolve(packDir, manifest, slot)
                ?.takeIf { CocoonWallpaperResolver.kindOf(it) == WallpaperKind.VIDEO }
                ?: continue
            writeStill(video, stillFile(packDir, slot))
        }
    }

    private fun writeStill(video: File, target: File) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(video.absolutePath)
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return
            target.parentFile?.mkdirs()
            target.outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, STILL_QUALITY, it) }
            frame.recycle()
        } catch (e: Exception) {
            Log.w(TAG, "No still frame for ${video.name}: ${e.message}")
        } finally {
            runCatching { retriever.release() }
        }
    }

    private companion object {
        const val TAG = "HelaThemeStore"
        const val STILLS_DIR = ".hela"
        const val STILL_QUALITY = 90
    }
}
