package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.data.model.hela.PackIconOverlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CocoonIconsTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun pack(vararg files: String): File {
        val dir = temp.newFolder("pack")
        files.forEach { relative ->
            File(dir, relative).apply { parentFile?.mkdirs() }.writeText("x")
        }
        return dir
    }

    private fun icons(vararg platforms: String): Map<String, PackIconOverlay> =
        platforms.associateWith { PackIconOverlay(maskPath = "/p/$it/mask.png", overlayPath = "/p/$it/overlay.png") }

    @Test
    fun `global only pack yields one overlay`() {
        val dir = pack("icon_overlays/global/mask.png", "icon_overlays/global/overlay.png")

        val found = CocoonIcons.find(dir)

        assertEquals(setOf("global"), found.keys)
        assertTrue(checkNotNull(found["global"]?.maskPath).endsWith("mask.png"))
        assertTrue(checkNotNull(found["global"]?.overlayPath).endsWith("overlay.png"))
    }

    @Test
    fun `per platform folders are keyed by their lower case name`() {
        val dir = pack(
            "icon_overlays/GBA/mask.png",
            "icon_overlays/GBA/overlay.png",
            "icon_overlays/tg-16/overlay.webp"
        )

        val found = CocoonIcons.find(dir)

        assertEquals(setOf("gba", "tg-16"), found.keys)
        assertNull(found["tg-16"]?.maskPath)
        assertTrue(checkNotNull(found["tg-16"]?.overlayPath).endsWith("overlay.webp"))
    }

    @Test
    fun `folders without mask or overlay are left out`() {
        val dir = pack("icon_overlays/gba/readme.txt", "icon_overlays/nes/other.png")

        assertTrue(CocoonIcons.find(dir).isEmpty())
    }

    @Test
    fun `pack without an icon folder has no icons`() {
        assertTrue(CocoonIcons.find(pack("sounds/navigate.ogg")).isEmpty())
    }

    @Test
    fun `same name platforms resolve directly`() {
        val pack = icons("gba", "switch", "psx", "global")

        assertEquals("/p/gba/mask.png", CocoonIcons.resolve(pack, "gba")?.maskPath)
        assertEquals("/p/switch/mask.png", CocoonIcons.resolve(pack, "switch")?.maskPath)
        assertEquals("/p/psx/mask.png", CocoonIcons.resolve(pack, "PSX")?.maskPath)
    }

    @Test
    fun `argosy slugs map to the cocoon names that differ`() {
        val pack = icons("n3ds", "psvita", "mastersystem", "gamegear", "sega32x", "tg-16", "wonderswancolor")

        assertEquals("/p/n3ds/mask.png", CocoonIcons.resolve(pack, "3ds")?.maskPath)
        assertEquals("/p/psvita/mask.png", CocoonIcons.resolve(pack, "vita")?.maskPath)
        assertEquals("/p/mastersystem/mask.png", CocoonIcons.resolve(pack, "sms")?.maskPath)
        assertEquals("/p/gamegear/mask.png", CocoonIcons.resolve(pack, "gg")?.maskPath)
        assertEquals("/p/sega32x/mask.png", CocoonIcons.resolve(pack, "32x")?.maskPath)
        assertEquals("/p/tg-16/mask.png", CocoonIcons.resolve(pack, "tg16")?.maskPath)
        assertEquals("/p/wonderswancolor/mask.png", CocoonIcons.resolve(pack, "wsc")?.maskPath)
    }

    @Test
    fun `server slug aliases resolve through the canonical slug`() {
        val pack = icons("n3ds", "nes")

        assertEquals("/p/n3ds/mask.png", CocoonIcons.resolve(pack, "nintendo-3ds")?.maskPath)
        assertEquals("/p/nes/mask.png", CocoonIcons.resolve(pack, "famicom")?.maskPath)
    }

    @Test
    fun `first existing cocoon name wins when a platform has several`() {
        assertEquals("/p/megacd/mask.png", CocoonIcons.resolve(icons("megacd"), "scd")?.maskPath)
        assertEquals("/p/segacd/mask.png", CocoonIcons.resolve(icons("megacd", "segacd"), "scd")?.maskPath)
    }

    @Test
    fun `unknown platform falls back to the global overlay`() {
        assertEquals("/p/global/mask.png", CocoonIcons.resolve(icons("gba", "global"), "ps5")?.maskPath)
    }

    @Test
    fun `platform without overlay and no global has none`() {
        assertNull(CocoonIcons.resolve(icons("gba"), "ps5"))
    }
}
