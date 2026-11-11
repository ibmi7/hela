package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.data.model.hela.CocoonThemeManifest
import com.nendo.argosy.data.model.hela.WallpaperKind
import com.nendo.argosy.data.model.hela.WallpaperSlot
import java.io.File

object CocoonWallpaperResolver {

    const val WALLPAPERS_DIR = "wallpapers"

    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif")
    private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "mov", "m4v")

    fun kindOf(file: File): WallpaperKind? = when (file.extension.lowercase()) {
        in IMAGE_EXTENSIONS -> WallpaperKind.IMAGE
        in VIDEO_EXTENSIONS -> WallpaperKind.VIDEO
        else -> null
    }

    /**
     * Wallpaper file of [slot] inside [packDir]: the manifest reference first, with or without
     * its extension, then the slot's conventional file names under wallpapers/.
     */
    fun resolve(packDir: File, manifest: CocoonThemeManifest, slot: WallpaperSlot): File? {
        val wallpapersDir = File(packDir, WALLPAPERS_DIR)
        val reference = manifest.wallpaperReference(slot)
        if (reference != null) {
            val declared = sequenceOf(File(wallpapersDir, reference), File(packDir, reference))
                .firstNotNullOfOrNull { candidate -> existingMedia(packDir, candidate) }
            if (declared != null) return declared
        }
        return slot.fallbackNames.firstNotNullOfOrNull { name ->
            existingMedia(packDir, File(wallpapersDir, name))
        }
    }

    private fun existingMedia(packDir: File, candidate: File): File? {
        if (!isInside(packDir, candidate)) return null
        if (candidate.isFile && kindOf(candidate) != null) return candidate
        val siblings = candidate.parentFile?.listFiles() ?: return null
        return siblings
            .filter { it.isFile && it.nameWithoutExtension == candidate.name && kindOf(it) != null }
            .minByOrNull { it.name }
    }

    private fun isInside(packDir: File, candidate: File): Boolean =
        candidate.canonicalPath.startsWith(packDir.canonicalPath + File.separator)
}
