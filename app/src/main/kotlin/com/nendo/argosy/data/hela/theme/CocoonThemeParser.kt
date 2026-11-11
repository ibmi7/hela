package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.data.model.hela.CocoonThemeManifest
import org.json.JSONException
import org.json.JSONObject

object CocoonThemeParser {

    /**
     * Manifest of a Cocoon theme.json. A missing or unreadable document yields a manifest
     * named [fallbackName] with no wallpaper references, leaving resolution to file names.
     */
    fun parse(json: String?, fallbackName: String): CocoonThemeManifest {
        val root = json?.let { text ->
            try {
                JSONObject(text)
            } catch (e: JSONException) {
                null
            }
        } ?: return CocoonThemeManifest(name = fallbackName)
        return CocoonThemeManifest(
            name = root.text("name") ?: fallbackName,
            author = root.text("author"),
            version = root.text("version"),
            description = root.text("description"),
            wallpaperMain = root.text("wallpaper_main"),
            wallpaperExternal = root.text("wallpaper_external")
        )
    }

    private fun JSONObject.text(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return get(key).toString().trim().takeIf { it.isNotEmpty() }
    }
}
