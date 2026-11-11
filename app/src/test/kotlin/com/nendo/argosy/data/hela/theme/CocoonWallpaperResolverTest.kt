package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.data.model.hela.CocoonThemeManifest
import com.nendo.argosy.data.model.hela.WallpaperKind
import com.nendo.argosy.data.model.hela.WallpaperSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CocoonWallpaperResolverTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun pack(vararg files: String): File {
        val dir = temp.newFolder("pack")
        files.forEach { relative ->
            File(dir, relative).apply { parentFile?.mkdirs() }.writeText("x")
        }
        return dir
    }

    @Test
    fun `declared references resolve under wallpapers`() {
        val dir = pack("wallpapers/main.mp4", "wallpapers/external.png")
        val manifest = CocoonThemeManifest("n", wallpaperMain = "main.mp4", wallpaperExternal = "external.png")

        assertEquals("main.mp4", CocoonWallpaperResolver.resolve(dir, manifest, WallpaperSlot.MAIN)?.name)
        assertEquals("external.png", CocoonWallpaperResolver.resolve(dir, manifest, WallpaperSlot.EXTERNAL)?.name)
    }

    @Test
    fun `reference without extension matches the file`() {
        val dir = pack("wallpapers/night.webm")
        val manifest = CocoonThemeManifest("n", wallpaperMain = "night")

        assertEquals("night.webm", CocoonWallpaperResolver.resolve(dir, manifest, WallpaperSlot.MAIN)?.name)
    }

    @Test
    fun `reference relative to the pack folder resolves`() {
        val dir = pack("art/bg.jpg")
        val manifest = CocoonThemeManifest("n", wallpaperExternal = "art/bg.jpg")

        assertEquals("bg.jpg", CocoonWallpaperResolver.resolve(dir, manifest, WallpaperSlot.EXTERNAL)?.name)
    }

    @Test
    fun `conventional names are found without references`() {
        val dir = pack("wallpapers/bottom.png", "wallpapers/top.mp4")
        val manifest = CocoonThemeManifest("n")

        assertEquals("bottom.png", CocoonWallpaperResolver.resolve(dir, manifest, WallpaperSlot.MAIN)?.name)
        assertEquals("top.mp4", CocoonWallpaperResolver.resolve(dir, manifest, WallpaperSlot.EXTERNAL)?.name)
    }

    @Test
    fun `missing declared file falls back to the conventional name`() {
        val dir = pack("wallpapers/main.png")
        val manifest = CocoonThemeManifest("n", wallpaperMain = "gone.mp4")

        assertEquals("main.png", CocoonWallpaperResolver.resolve(dir, manifest, WallpaperSlot.MAIN)?.name)
    }

    @Test
    fun `reference outside the pack folder is ignored`() {
        val dir = pack("wallpapers/placeholder.txt")
        File(dir.parentFile, "outside.png").writeText("x")
        val manifest = CocoonThemeManifest("n", wallpaperMain = "../../outside.png")

        assertNull(CocoonWallpaperResolver.resolve(dir, manifest, WallpaperSlot.MAIN))
    }

    @Test
    fun `non media files are not wallpapers`() {
        val dir = pack("wallpapers/main.txt")

        assertNull(CocoonWallpaperResolver.resolve(dir, CocoonThemeManifest("n"), WallpaperSlot.MAIN))
    }

    @Test
    fun `kind follows the extension`() {
        assertEquals(WallpaperKind.VIDEO, CocoonWallpaperResolver.kindOf(File("a.MP4")))
        assertEquals(WallpaperKind.VIDEO, CocoonWallpaperResolver.kindOf(File("a.webm")))
        assertEquals(WallpaperKind.IMAGE, CocoonWallpaperResolver.kindOf(File("a.png")))
        assertNull(CocoonWallpaperResolver.kindOf(File("a.json")))
    }
}
