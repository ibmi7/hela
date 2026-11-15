package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.data.model.hela.PackFolderArt
import com.nendo.argosy.data.model.hela.PackSmartFolders
import java.io.File

object CocoonSmartFolders {

    const val FOLDERS_DIR = "smart_folders"
    const val FAVORITES = "favorites"
    const val RECENT = "recent"
    const val MOST_PLAYED = "most_played"
    const val NEWLY_ADDED = "newly_added"
    private const val BY_PLATFORM = "by_platform"
    private const val ICON = "icon"
    private const val LOGO = "logo"
    private const val HERO = "hero"

    private val IMAGE_EXTENSIONS = setOf("png", "webp", "jpg", "jpeg", "gif")

    private val SPECIAL_PRIORITY = listOf(FAVORITES, RECENT, MOST_PLAYED, NEWLY_ADDED)

    /**
     * Folder art under [packDir]/smart_folders: one entry per platform folder of by_platform and
     * one per other folder type, each keyed by its folder name in lower case. A folder holding
     * none of icon, logo and hero is left out. A special folder whose icon is the same image as
     * the icon of a special folder ranked before it (favorites, recent, most_played,
     * newly_added, then the others by name) has no icon.
     */
    fun find(packDir: File): PackSmartFolders {
        val root = File(packDir, FOLDERS_DIR)
        val platforms = artByFolder(File(root, BY_PLATFORM))
        val special = distinctIcons(artByFolder(root).filterKeys { it != BY_PLATFORM })
        return PackSmartFolders(platforms = platforms, special = special)
    }

    private fun distinctIcons(special: Map<String, PackFolderArt>): Map<String, PackFolderArt> {
        val ranked = special.keys.sortedWith(
            compareBy({ SPECIAL_PRIORITY.indexOf(it).let { rank -> if (rank < 0) SPECIAL_PRIORITY.size else rank } }, { it })
        )
        val kept = ArrayList<File>()
        val result = LinkedHashMap<String, PackFolderArt>()
        for (key in ranked) {
            val art = special.getValue(key)
            val icon = art.iconPath?.let(::File)
            val duplicate = icon != null && kept.any { sameImage(it, icon) }
            if (icon != null && !duplicate) kept += icon
            val distinct = if (duplicate) art.copy(iconPath = null) else art
            if (distinct.iconPath == null && distinct.logoPath == null && distinct.heroPath == null) continue
            result[key] = distinct
        }
        return result
    }

    private fun sameImage(a: File, b: File): Boolean =
        a.length() == b.length() && runCatching { a.readBytes().contentEquals(b.readBytes()) }.getOrDefault(false)

    /**
     * Art for an Argosy platform slug, looked up under the names [CocoonIcons.cocoonNames] gives.
     */
    fun resolve(folders: PackSmartFolders, platformSlug: String): PackFolderArt? =
        CocoonIcons.cocoonNames(platformSlug).firstNotNullOfOrNull { folders.platforms[it] }

    private fun artByFolder(parent: File): Map<String, PackFolderArt> {
        val found = LinkedHashMap<String, PackFolderArt>()
        val folders = parent.listFiles().orEmpty().filter { it.isDirectory }.sortedBy { it.name }
        for (folder in folders) {
            val images = folder.listFiles().orEmpty()
                .filter { it.isFile && it.extension.lowercase() in IMAGE_EXTENSIONS }
                .sortedBy { it.name }
            fun named(name: String) = images.firstOrNull { it.nameWithoutExtension.lowercase() == name }?.absolutePath
            val art = PackFolderArt(iconPath = named(ICON), logoPath = named(LOGO), heroPath = named(HERO))
            if (art.iconPath == null && art.logoPath == null && art.heroPath == null) continue
            found[folder.name.lowercase()] = art
        }
        return found
    }
}
