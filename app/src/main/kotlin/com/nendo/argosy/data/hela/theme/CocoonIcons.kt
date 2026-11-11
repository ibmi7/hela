package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.data.model.hela.PackIconOverlay
import com.nendo.argosy.data.platform.PlatformDefinitions
import java.io.File

object CocoonIcons {

    const val ICONS_DIR = "icon_overlays"
    const val GLOBAL = "global"
    private const val MASK = "mask"
    private const val OVERLAY = "overlay"

    private val IMAGE_EXTENSIONS = setOf("png", "webp", "jpg", "jpeg")

    /**
     * Cocoon platform folder names for the Argosy slugs that Cocoon names differently. A slug
     * that is absent here is looked up under its own name.
     */
    private val COCOON_IDS: Map<String, List<String>> = mapOf(
        "3ds" to listOf("n3ds"),
        "dsi" to listOf("ndsi"),
        "vb" to listOf("virtualboy"),
        "vita" to listOf("psvita"),
        "sg1000" to listOf("sg-1000"),
        "sms" to listOf("mastersystem"),
        "scd" to listOf("segacd", "megacd"),
        "32x" to listOf("sega32x"),
        "gg" to listOf("gamegear"),
        "jaguar" to listOf("atarijaguar"),
        "jaguarcd" to listOf("atarijaguarcd"),
        "lynx" to listOf("atarilynx"),
        "tg16" to listOf("tg-16"),
        "tgcd" to listOf("tg-cd"),
        "appleii" to listOf("apple2"),
        "pc8800" to listOf("pc88"),
        "pc9800" to listOf("pc98"),
        "zx" to listOf("zxspectrum"),
        "cdi" to listOf("cdimono", "cdimono1"),
        "coleco" to listOf("colecovision"),
        "wsc" to listOf("wonderswancolor"),
        "android" to listOf("android_app"),
        "arcade" to listOf("mame"),
        "pc" to listOf("windows"),
        "msx2" to listOf("msx")
    )

    /**
     * Overlay per Cocoon platform folder under [packDir]/icon_overlays, keyed by the folder name
     * in lower case. A folder with neither a mask nor an overlay image is left out.
     */
    fun find(packDir: File): Map<String, PackIconOverlay> {
        val folders = File(packDir, ICONS_DIR).listFiles().orEmpty().filter { it.isDirectory }
        val found = LinkedHashMap<String, PackIconOverlay>()
        for (folder in folders.sortedBy { it.name }) {
            val images = folder.listFiles().orEmpty()
                .filter { it.isFile && it.extension.lowercase() in IMAGE_EXTENSIONS }
                .sortedBy { it.name }
            val mask = images.firstOrNull { it.nameWithoutExtension.lowercase() == MASK }
            val overlay = images.firstOrNull { it.nameWithoutExtension.lowercase() == OVERLAY }
            if (mask == null && overlay == null) continue
            found[folder.name.lowercase()] = PackIconOverlay(
                maskPath = mask?.absolutePath,
                overlayPath = overlay?.absolutePath
            )
        }
        return found
    }

    /**
     * Overlay for an Argosy platform slug: the slug itself, then its canonical slug, then the
     * Cocoon names of that platform, then the pack's global overlay. Null when none exists.
     */
    fun resolve(icons: Map<String, PackIconOverlay>, platformSlug: String): PackIconOverlay? {
        val slug = platformSlug.lowercase()
        val canonical = PlatformDefinitions.getCanonicalSlug(slug)
        val candidates = listOf(slug, canonical) + COCOON_IDS[canonical].orEmpty()
        return candidates.firstNotNullOfOrNull { icons[it] } ?: icons[GLOBAL]
    }
}
