package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.data.model.hela.CocoonThemeManifest
import com.nendo.argosy.data.model.hela.CocoonThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CocoonColorsTest {

    private fun fixture(name: String): CocoonThemeManifest {
        val json = checkNotNull(javaClass.getResourceAsStream("/hela/$name")).bufferedReader().use { it.readText() }
        return CocoonThemeParser.parse(json, "fallback")
    }

    private fun argb(value: Long): Int = value.toInt()

    @Test
    fun `six digit hex is opaque`() {
        assertEquals(argb(0xFF008BB5), CocoonColors.parse("#008BB5"))
        assertEquals(argb(0xFF27AE60), CocoonColors.parse("#27ae60"))
    }

    @Test
    fun `eight digit hex leads with alpha`() {
        assertEquals(argb(0xFF86A7FD), CocoonColors.parse("#FF86A7FD"))
        assertEquals(argb(0x1AFFFFFF), CocoonColors.parse("#1AFFFFFF"))
    }

    @Test
    fun `three digit hex doubles each digit`() {
        assertEquals(argb(0xFFFFAA00), CocoonColors.parse("#FA0"))
    }

    @Test
    fun `rgba function carries a fractional alpha`() {
        assertEquals(argb(0x80000000), CocoonColors.parse("rgba(0,0,0,0.5)"))
        assertEquals(argb(0x4DE390F8), CocoonColors.parse("rgba(227, 144, 248, 0.3)"))
        assertEquals(argb(0xFF0A141E), CocoonColors.parse("rgb(10, 20, 30)"))
    }

    @Test
    fun `other text is not a color`() {
        assertNull(CocoonColors.parse(null))
        assertNull(CocoonColors.parse(""))
        assertNull(CocoonColors.parse("teal"))
        assertNull(CocoonColors.parse("#12345"))
        assertNull(CocoonColors.parse("#GGGGGG"))
        assertNull(CocoonColors.parse("rgba(300,0,0,1)"))
        assertNull(CocoonColors.parse("rgba(0,0,0,2)"))
        assertNull(CocoonColors.parse("rgb(1,2)"))
    }

    @Test
    fun `3ds pack is dark with its selection color as accent`() {
        val colors = checkNotNull(CocoonColors.packColors(fixture("theme-3ds-dark-reimagined.json")))

        assertEquals(CocoonThemeMode.DARK, colors.mode)
        assertEquals(true, colors.isDark)
        assertEquals(argb(0xFF5CE6D7), colors.accent)
        assertEquals(argb(0xFF1FFFC2), colors.secondary)
    }

    @Test
    fun `iisu pack is oled with its accent gradient`() {
        val colors = checkNotNull(CocoonColors.packColors(fixture("theme-iisu-interpreted.json")))

        assertEquals(CocoonThemeMode.OLED, colors.mode)
        assertEquals(true, colors.isDark)
        assertEquals(argb(0xFF008BB5), colors.accent)
        assertEquals(argb(0xFF00A865), colors.secondary)
    }

    @Test
    fun `oled observatory pack takes its accent gradient`() {
        val colors = checkNotNull(CocoonColors.packColors(fixture("theme-oled-observatory.json")))

        assertEquals(CocoonThemeMode.OLED, colors.mode)
        assertEquals(argb(0xFF86A7FD), colors.accent)
        assertEquals(argb(0xFF8037FF), colors.secondary)
    }

    @Test
    fun `translucent glow becomes an opaque accent`() {
        val manifest = CocoonThemeManifest("n", colorScheme = mapOf("accent_glow" to "rgba(227,144,248,0.3)"))

        assertEquals(argb(0xFFE390F8), CocoonColors.packColors(manifest)?.accent)
    }

    @Test
    fun `light and system modes resolve as documented`() {
        assertEquals(false, CocoonColors.packColors(CocoonThemeManifest("n", themeMode = "LIGHT"))?.isDark)
        assertNull(CocoonColors.packColors(CocoonThemeManifest("n", themeMode = "system"))?.isDark)
    }

    @Test
    fun `theme without mode or accent has no colors`() {
        assertNull(CocoonColors.packColors(CocoonThemeManifest("n")))
        assertNull(CocoonColors.packColors(CocoonThemeManifest("n", themeMode = "sepia")))
    }
}
