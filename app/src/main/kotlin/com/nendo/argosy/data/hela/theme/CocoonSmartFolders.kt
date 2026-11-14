package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.data.model.hela.PackFolderArt
import com.nendo.argosy.data.model.hela.PackSmartFolders
import java.io.File

object CocoonSmartFolders {

    const val FOLDERS_DIR = "smart_folders"
    const val FAVORITES = "favorites"
    const val RECENT = "recent"
    private const val BY_PLATFORM = "by_platform"
    private const val ICON = "icon"
    private const val LOGO = "logo"
    private const val HERO = "hero"

    private val IMAGE_EXTENSIONS = setOf("png", "webp", "jpg", "jpeg", "gif")

    /**
     * Folder art under [packDir]/smart_folders: one entry per platform folder of by_platform and
     * one per other folder type, each keyed by its folder name in lower case. A folder holding
     * none of icon, logo and hero is left out.
     */
    fun find(packDir: File): PackSmartFolders {
        val root = File(packDir, FOLDERS_DIR)
        val platforms = artByFolder(File(root, BY_PLATFORM))
        val special = artByFolder(root).filterKeys { it != BY_PLATFORM }
        return PackSmartFolders(platforms = platforms, special = special)
    }

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
