package com.nendo.argosy.data.model.hela

enum class WallpaperSlot(val token: String, val fallbackNames: List<String>) {
    MAIN("main", listOf("main", "bottom")),
    EXTERNAL("external", listOf("external", "top"))
}

enum class WallpaperKind { IMAGE, VIDEO }

enum class CocoonThemeMode(val token: String) {
    LIGHT("light"),
    DARK("dark"),
    OLED("oled"),
    SYSTEM("system");

    companion object {
        fun fromToken(value: String?): CocoonThemeMode? =
            entries.firstOrNull { it.token == value?.trim()?.lowercase() }
    }
}

data class PackColors(
    val mode: CocoonThemeMode? = null,
    val accent: Int? = null,
    val secondary: Int? = null
) {
    val isDark: Boolean?
        get() = when (mode) {
            CocoonThemeMode.DARK, CocoonThemeMode.OLED -> true
            CocoonThemeMode.LIGHT -> false
            CocoonThemeMode.SYSTEM, null -> null
        }
}

data class CocoonThemeManifest(
    val name: String,
    val author: String? = null,
    val version: String? = null,
    val description: String? = null,
    val wallpaperMain: String? = null,
    val wallpaperExternal: String? = null,
    val themeMode: String? = null,
    val colorScheme: Map<String, String> = emptyMap(),
    val category: String? = null,
    val musicMode: String? = null,
    val musicPlaylist: String? = null
) {
    fun wallpaperReference(slot: WallpaperSlot): String? = when (slot) {
        WallpaperSlot.MAIN -> wallpaperMain
        WallpaperSlot.EXTERNAL -> wallpaperExternal
    }
}

data class PackTrack(
    val title: String,
    val path: String
)

data class PackWallpaper(
    val path: String,
    val kind: WallpaperKind,
    val stillPath: String? = null
)

data class InstalledThemePack(
    val id: String,
    val name: String,
    val author: String? = null,
    val version: String? = null,
    val main: PackWallpaper? = null,
    val external: PackWallpaper? = null,
    val colors: PackColors? = null,
    val sounds: Map<String, String> = emptyMap(),
    val music: List<PackTrack> = emptyList()
) {
    fun wallpaper(slot: WallpaperSlot): PackWallpaper? = when (slot) {
        WallpaperSlot.MAIN -> main
        WallpaperSlot.EXTERNAL -> external
    }
}

data class HelaThemeSelection(
    val mainPackId: String? = null,
    val externalPackId: String? = null,
    val colorsPackId: String? = null,
    val soundsPackId: String? = null,
    val musicPackId: String? = null,
    val animate: Boolean = true
) {
    val isEmpty: Boolean
        get() = mainPackId == null && externalPackId == null && colorsPackId == null &&
            soundsPackId == null && musicPackId == null

    fun packId(slot: WallpaperSlot): String? = when (slot) {
        WallpaperSlot.MAIN -> mainPackId
        WallpaperSlot.EXTERNAL -> externalPackId
    }
}

data class HelaWallpapers(
    val main: PackWallpaper? = null,
    val external: PackWallpaper? = null,
    val animate: Boolean = true
)
