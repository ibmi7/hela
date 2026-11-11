package com.nendo.argosy.data.hela.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CocoonPackImporterTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun zip(name: String, vararg entries: Pair<String, String>): File {
        val file = temp.newFile(name)
        ZipOutputStream(file.outputStream()).use { out ->
            entries.forEach { (path, content) ->
                out.putNextEntry(ZipEntry(path))
                out.write(content.toByteArray())
                out.closeEntry()
            }
        }
        return file
    }

    private fun success(result: PackImportResult): File {
        assertTrue("expected success, got $result", result is PackImportResult.Success)
        return (result as PackImportResult.Success).packDir
    }

    private fun failure(result: PackImportResult): PackImportFailure {
        assertTrue("expected failure, got $result", result is PackImportResult.Failure)
        return (result as PackImportResult.Failure).reason
    }

    @Test
    fun `silk layout extracts the theme folder under its own id`() {
        val themes = temp.newFolder("themes")
        val archive = zip(
            "pack.zip",
            "themes/oled-observatory-7c3f1159/theme.json" to """{"name":"OLED Observatory"}""",
            "themes/oled-observatory-7c3f1159/wallpapers/external.png" to "png",
            "themes/oled-observatory-7c3f1159/wallpapers/main.mp4" to "mp4"
        )

        val dir = success(CocoonPackImporter.import(archive, themes))

        assertEquals("oled-observatory-7c3f1159", dir.name)
        assertEquals(themes, dir.parentFile)
        assertTrue(File(dir, "theme.json").isFile)
        assertEquals("mp4", File(dir, "wallpapers/main.mp4").readText())
        assertFalse(File(themes, ".staging-oled-observatory-7c3f1159").exists())
    }

    @Test
    fun `root level theme takes the archive name as id`() {
        val themes = temp.newFolder("themes")
        val archive = zip("My Pack!.zip", "theme.json" to "{}", "wallpapers/main.png" to "png")

        val dir = success(CocoonPackImporter.import(archive, themes))

        assertEquals("My_Pack_", dir.name)
        assertTrue(File(dir, "wallpapers/main.png").isFile)
    }

    @Test
    fun `files outside the theme folder are left out`() {
        val themes = temp.newFolder("themes")
        val archive = zip(
            "pack.zip",
            "readme.txt" to "hello",
            "themes/a/theme.json" to "{}",
            "themes/b/theme.json" to "{}"
        )

        val dir = success(CocoonPackImporter.import(archive, themes))

        assertEquals("a", dir.name)
        assertFalse(File(themes, "b").exists())
        assertFalse(File(dir, "readme.txt").exists())
    }

    @Test
    fun `archive without theme json is placed from its wallpapers folder`() {
        val themes = temp.newFolder("themes")
        val archive = zip("loose.zip", "stuff/wallpapers/main.png" to "png")

        val dir = success(CocoonPackImporter.import(archive, themes))

        assertEquals("stuff", dir.name)
        assertTrue(File(dir, "wallpapers/main.png").isFile)
    }

    @Test
    fun `archive with neither theme nor wallpapers is refused`() {
        val themes = temp.newFolder("themes")
        val archive = zip("other.zip", "notes/readme.txt" to "hello")

        assertEquals(PackImportFailure.NO_THEME, failure(CocoonPackImporter.import(archive, themes)))
        assertTrue(themes.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `entry escaping the theme folder is refused and nothing is kept`() {
        val themes = temp.newFolder("themes")
        val archive = zip(
            "evil.zip",
            "themes/x/theme.json" to "{}",
            "themes/x/../../../escaped.png" to "png"
        )

        assertEquals(PackImportFailure.UNSAFE_ENTRY, failure(CocoonPackImporter.import(archive, themes)))
        assertTrue(themes.listFiles().orEmpty().isEmpty())
        assertFalse(File(temp.root, "escaped.png").exists())
    }

    @Test
    fun `file that is not a zip is refused`() {
        val themes = temp.newFolder("themes")
        val notZip = temp.newFile("pack.zip").apply { writeText("plain text") }

        assertEquals(PackImportFailure.NOT_A_ZIP, failure(CocoonPackImporter.import(notZip, themes)))
    }

    @Test
    fun `importing the same pack again replaces its files`() {
        val themes = temp.newFolder("themes")
        val first = zip("v1.zip", "themes/p/theme.json" to "{}", "themes/p/wallpapers/old.png" to "old")
        val second = zip("v2.zip", "themes/p/theme.json" to "{}", "themes/p/wallpapers/new.png" to "new")

        success(CocoonPackImporter.import(first, themes))
        val dir = success(CocoonPackImporter.import(second, themes))

        assertFalse(File(dir, "wallpapers/old.png").exists())
        assertTrue(File(dir, "wallpapers/new.png").isFile)
    }
}
