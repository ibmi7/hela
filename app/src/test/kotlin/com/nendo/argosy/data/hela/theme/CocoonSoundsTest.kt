package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.core.input.SoundType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CocoonSoundsTest {

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
    fun `recognised names are found with any supported format`() {
        val dir = pack("sounds/navigate.ogg", "sounds/Select.WAV", "sounds/back.mp3", "sounds/launch.flac")

        val sounds = CocoonSounds.find(dir)

        assertEquals(setOf("navigate", "select", "back", "launch"), sounds.keys)
        assertTrue(checkNotNull(sounds["select"]).endsWith("Select.WAV"))
    }

    @Test
    fun `unknown names and non audio files are left out`() {
        val dir = pack("sounds/whoosh.ogg", "sounds/navigate.txt", "sounds/readme.md", "navigate.ogg")

        assertTrue(CocoonSounds.find(dir).isEmpty())
    }

    @Test
    fun `sounds one folder deeper are found`() {
        val dir = pack("sounds/my-pack/navigate.ogg", "sounds/my-pack/error.wav")

        assertEquals(setOf("navigate", "error"), CocoonSounds.find(dir).keys)
    }

    @Test
    fun `a direct file wins over a nested one`() {
        val dir = pack("sounds/navigate.wav", "sounds/alt/navigate.ogg")

        assertTrue(checkNotNull(CocoonSounds.find(dir)["navigate"]).endsWith("sounds${File.separator}navigate.wav"))
    }

    @Test
    fun `pack without a sounds folder has no sounds`() {
        assertTrue(CocoonSounds.find(pack("wallpapers/main.png")).isEmpty())
    }

    @Test
    fun `cocoon names map to argosy events`() {
        val configs = CocoonSounds.configs(
            mapOf(
                "navigate" to "/p/navigate.ogg",
                "select" to "/p/select.ogg",
                "back" to "/p/back.ogg",
                "folder_open" to "/p/open.ogg",
                "folder_close" to "/p/close.ogg",
                "launch" to "/p/launch.ogg",
                "error" to "/p/error.ogg"
            )
        )

        assertEquals("/p/navigate.ogg", configs[SoundType.NAVIGATE]?.customFilePath)
        assertEquals("/p/select.ogg", configs[SoundType.SELECT]?.customFilePath)
        assertEquals("/p/back.ogg", configs[SoundType.BACK]?.customFilePath)
        assertEquals("/p/open.ogg", configs[SoundType.OPEN_MODAL]?.customFilePath)
        assertEquals("/p/close.ogg", configs[SoundType.CLOSE_MODAL]?.customFilePath)
        assertEquals("/p/launch.ogg", configs[SoundType.LAUNCH_GAME]?.customFilePath)
        assertEquals("/p/error.ogg", configs[SoundType.ERROR]?.customFilePath)
        assertEquals("/p/error.ogg", configs[SoundType.BOUNDARY]?.customFilePath)
    }

    @Test
    fun `cocoon sounds without an argosy event are not mapped`() {
        val configs = CocoonSounds.configs(
            mapOf("discord_open" to "/p/a.ogg", "saving" to "/p/b.ogg", "retroachievements" to "/p/c.ogg")
        )

        assertTrue(configs.isEmpty())
    }

    @Test
    fun `events the pack does not cover stay unset`() {
        val configs = CocoonSounds.configs(mapOf("navigate" to "/p/navigate.ogg"))

        assertNull(configs[SoundType.SELECT])
        assertFalse(configs.containsKey(SoundType.FAVORITE))
    }

    @Test
    fun `silk asset pack layout imports under its pack id`() {
        val themes = temp.newFolder("themes")
        val archive = temp.newFile("sounds.zip")
        ZipOutputStream(archive.outputStream()).use { out ->
            listOf(
                "assets/sounds/3ds-sound-effects-d750b4a4/metadata.json" to """{"name":"3DS Sound Effects","category":"sounds"}""",
                "assets/sounds/3ds-sound-effects-d750b4a4/sounds/navigate.ogg" to "ogg",
                "assets/sounds/3ds-sound-effects-d750b4a4/sounds/select.ogg" to "ogg"
            ).forEach { (path, content) ->
                out.putNextEntry(ZipEntry(path))
                out.write(content.toByteArray())
                out.closeEntry()
            }
        }

        val result = CocoonPackImporter.import(archive, themes)

        assertTrue("expected success, got $result", result is PackImportResult.Success)
        val dir = (result as PackImportResult.Success).packDir
        assertEquals("3ds-sound-effects-d750b4a4", dir.name)
        assertTrue(File(dir, "metadata.json").isFile)
        assertEquals(setOf("navigate", "select"), CocoonSounds.find(dir).keys)
        val manifest = CocoonThemeParser.parse(File(dir, "metadata.json").readText(), dir.name)
        assertEquals("3DS Sound Effects", manifest.name)
        assertEquals("sounds", manifest.category)
    }

    @Test
    fun `archive with only a sounds folder is placed from it`() {
        val themes = temp.newFolder("themes")
        val archive = temp.newFile("loose.zip")
        ZipOutputStream(archive.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("mine/sounds/back.wav"))
            out.write("wav".toByteArray())
            out.closeEntry()
        }

        val result = CocoonPackImporter.import(archive, themes)

        assertTrue("expected success, got $result", result is PackImportResult.Success)
        assertEquals("mine", (result as PackImportResult.Success).packDir.name)
    }
}
