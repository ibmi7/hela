package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.data.model.hela.CocoonThemeManifest
import com.nendo.argosy.data.model.hela.CocoonThemeMode
import com.nendo.argosy.data.model.hela.PackColors
import kotlin.math.roundToInt

object CocoonColors {

    private const val OPAQUE = 0xFF000000.toInt()
    private val ACCENT_KEYS = listOf("selection_gradient_start", "accent_gradient_start", "accent_glow")
    private val SECONDARY_KEYS = listOf("selection_gradient_end", "accent_gradient_end")
    private val FUNCTION = Regex("""rgba?\(([^)]*)\)""", RegexOption.IGNORE_CASE)

    /**
     * ARGB value of a Cocoon color: #RGB, #RRGGBB, #AARRGGBB, rgb(r,g,b) or rgba(r,g,b,a) with
     * a between 0 and 1. Null for any other text.
     */
    fun parse(text: String?): Int? {
        val value = text?.trim().orEmpty()
        if (value.startsWith("#")) return parseHex(value.drop(1))
        val arguments = FUNCTION.matchEntire(value)?.groupValues?.get(1) ?: return null
        val parts = arguments.split(',').map { it.trim() }
        if (parts.size !in 3..4) return null
        val channels = parts.take(3).map { it.toIntOrNull()?.takeIf { c -> c in 0..255 } ?: return null }
        val alpha = if (parts.size == 4) {
            val fraction = parts[3].toFloatOrNull()?.takeIf { it in 0f..1f } ?: return null
            (fraction * 255f).roundToInt()
        } else {
            255
        }
        return (alpha shl 24) or (channels[0] shl 16) or (channels[1] shl 8) or channels[2]
    }

    /**
     * Mode and accent pair of a theme. The accent is the selection color when the theme has one,
     * then the accent gradient start, then the glow; both colors are returned opaque. Null when
     * the theme sets neither a mode nor an accent.
     */
    fun packColors(manifest: CocoonThemeManifest): PackColors? {
        val mode = CocoonThemeMode.fromToken(manifest.themeMode)
        val accent = firstColor(manifest, ACCENT_KEYS)
        val secondary = firstColor(manifest, SECONDARY_KEYS)
        if (mode == null && accent == null) return null
        return PackColors(mode = mode, accent = accent, secondary = secondary)
    }

    private fun firstColor(manifest: CocoonThemeManifest, keys: List<String>): Int? =
        keys.firstNotNullOfOrNull { key -> parse(manifest.colorScheme[key]) }?.let { it or OPAQUE }

    private fun parseHex(digits: String): Int? {
        if (digits.any { it.digitToIntOrNull(16) == null }) return null
        return when (digits.length) {
            3 -> digits.map { "$it$it" }.joinToString("").toInt(16) or OPAQUE
            6 -> digits.toInt(16) or OPAQUE
            8 -> digits.toLong(16).toInt()
            else -> null
        }
    }
}
