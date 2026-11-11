package com.nendo.argosy.data.model.hela

enum class WallpaperSlot(val token: String, val fallbackNames: List<String>) {
    MAIN("main", listOf("main", "bottom")),
    EXTERNAL("external", listOf("external", "top"))
}

enum class WallpaperKind { IMAGE, VIDEO }

data class CocoonThemeManifest(
    val name: String,
    val author: String? = null,
    val version: String? = null,
    val description: String? = null,
    val wallpaperMain: String? = null,
    val wallpaperExternal: String? = null
) {
    fun wallpaperReference(slot: WallpaperSlot): String? = when (slot) {
        WallpaperSlot.MAIN -> wallpaperMain
        WallpaperSlot.EXTERNAL -> wallpaperExternal
    }
}

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
    val external: PackWallpaper? = null
) {
    fun wallpaper(slot: WallpaperSlot): PackWallpaper? = when (slot) {
        WallpaperSlot.MAIN -> main
        WallpaperSlot.EXTERNAL -> external
    }
}

data class HelaThemeSelection(
    val mainPackId: String? = null,
    val externalPackId: String? = null,
    val animate: Boolean = true
) {
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
