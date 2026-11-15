package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.data.model.hela.PackFolderArt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CocoonSmartFoldersTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun file(pack: File, path: String): String {
        val target = File(pack, path)
        target.parentFile?.mkdirs()
        target.writeBytes(byteArrayOf(1))
        return target.absolutePath
    }

    @Test
    fun `platform folders and special folders are read with their icon, logo and hero`() {
        val pack = temp.newFolder("pack")
        val switchIcon = file(pack, "smart_folders/by_platform/switch/icon.png")
        val switchLogo = file(pack, "smart_folders/by_platform/switch/logo.webp")
        val switchHero = file(pack, "smart_folders/by_platform/switch/hero.jpg")
        val vitaIcon = file(pack, "smart_folders/by_platform/PSVita/Icon.PNG")
        val favoritesIcon = file(pack, "smart_folders/favorites/icon.png")
        val recentHero = file(pack, "smart_folders/recent/hero.png")

        val folders = CocoonSmartFolders.find(pack)

        assertEquals(PackFolderArt(switchIcon, switchLogo, switchHero), folders.platforms["switch"])
        assertEquals(PackFolderArt(iconPath = vitaIcon), folders.platforms["psvita"])
        assertEquals(PackFolderArt(iconPath = favoritesIcon), folders.special[CocoonSmartFolders.FAVORITES])
        assertEquals(PackFolderArt(heroPath = recentHero), folders.special[CocoonSmartFolders.RECENT])
        assertEquals(setOf("favorites", "recent"), folders.special.keys)
    }

    @Test
    fun `stray files and folders without art are left out`() {
        val pack = temp.newFolder("pack")
        file(pack, "smart_folders/by_platform/switch/switch2.png")
        file(pack, "smart_folders/by_platform/wii/notes.txt")
        file(pack, "smart_folders/by_platform/icon.png")

        val folders = CocoonSmartFolders.find(pack)

        assertTrue(folders.isEmpty)
    }

    @Test
    fun `a pack without smart folders is empty`() {
        assertTrue(CocoonSmartFolders.find(temp.newFolder("pack")).isEmpty)
    }

    @Test
    fun `an Argosy slug resolves through its Cocoon names`() {
        val pack = temp.newFolder("pack")
        val vita = file(pack, "smart_folders/by_platform/psvita/icon.png")
        val n3ds = file(pack, "smart_folders/by_platform/n3ds/icon.png")
        val folders = CocoonSmartFolders.find(pack)

        assertEquals(vita, CocoonSmartFolders.resolve(folders, "psvita")?.iconPath)
        assertEquals(vita, CocoonSmartFolders.resolve(folders, "vita")?.iconPath)
        assertEquals(n3ds, CocoonSmartFolders.resolve(folders, "3ds")?.iconPath)
        assertNull(CocoonSmartFolders.resolve(folders, "switch"))
    }

    @Test
    fun `a special folder reusing the icon of one ranked before it has no icon`() {
        val pack = temp.newFolder("pack")
        val recent = file(pack, "smart_folders/recent/icon.png")
        file(pack, "smart_folders/newly_added/icon.png")
        val newlyAddedLogo = file(pack, "smart_folders/newly_added/logo.png")
        file(pack, "smart_folders/android_games/icon.png")
        val mostPlayed = File(pack, "smart_folders/most_played/icon.png")
        mostPlayed.parentFile?.mkdirs()
        mostPlayed.writeBytes(byteArrayOf(2))

        val folders = CocoonSmartFolders.find(pack)

        assertEquals(recent, folders.special["recent"]?.iconPath)
        assertEquals(mostPlayed.absolutePath, folders.special["most_played"]?.iconPath)
        assertEquals(PackFolderArt(logoPath = newlyAddedLogo), folders.special["newly_added"])
        assertNull(folders.special["android_games"])
    }
}
