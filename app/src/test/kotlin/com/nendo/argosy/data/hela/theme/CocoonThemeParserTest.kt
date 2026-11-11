package com.nendo.argosy.data.hela.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CocoonThemeParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/hela/$name")).bufferedReader().use { it.readText() }

    @Test
    fun `3ds pack declares a video main and an image external`() {
        val manifest = CocoonThemeParser.parse(fixture("theme-3ds-dark-reimagined.json"), "fallback")

        assertEquals("3DS Dark Reimagined", manifest.name)
        assertEquals("GiuPhoenix", manifest.author)
        assertEquals("1.0", manifest.version)
        assertEquals("main.mp4", manifest.wallpaperMain)
        assertEquals("external.png", manifest.wallpaperExternal)
    }

    @Test
    fun `iisu pack declares a webm external`() {
        val manifest = CocoonThemeParser.parse(fixture("theme-iisu-interpreted.json"), "fallback")

        assertEquals("Iisu interpreted theme", manifest.name)
        assertEquals("main.mp4", manifest.wallpaperMain)
        assertEquals("external.webm", manifest.wallpaperExternal)
    }

    @Test
    fun `oled observatory pack parses with a partial color scheme`() {
        val manifest = CocoonThemeParser.parse(fixture("theme-oled-observatory.json"), "fallback")

        assertEquals("OLED Observatory", manifest.name)
        assertEquals("Lyte", manifest.author)
        assertEquals("external.png", manifest.wallpaperExternal)
    }

    @Test
    fun `unreadable json keeps the fallback name and no references`() {
        val manifest = CocoonThemeParser.parse("{ not json", "my-pack")

        assertEquals("my-pack", manifest.name)
        assertNull(manifest.wallpaperMain)
        assertNull(manifest.wallpaperExternal)
    }

    @Test
    fun `missing document keeps the fallback name`() {
        assertEquals("my-pack", CocoonThemeParser.parse(null, "my-pack").name)
    }

    @Test
    fun `blank or absent name falls back`() {
        assertEquals("my-pack", CocoonThemeParser.parse("""{"name": "  "}""", "my-pack").name)
        assertEquals("my-pack", CocoonThemeParser.parse("""{"author": "x"}""", "my-pack").name)
    }

    @Test
    fun `numeric version is read as text`() {
        assertEquals("2", CocoonThemeParser.parse("""{"name": "n", "version": 2}""", "f").version)
    }
}
