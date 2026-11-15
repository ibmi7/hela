package com.nendo.argosy.data.emulator.savepath

import com.nendo.argosy.data.emulator.SavePathConfig
import com.nendo.argosy.data.storage.FileAccessLayer

/**
 * The save folder Vita3K writes to when its VitaFS has been moved: `pref-path` and `user-id` of
 * the `config.yml` Vita3K keeps in its app files folder name `<pref-path>/ux0/user/<id>/savedata`.
 */
object Vita3kPrefPath {
    private val CONFIG_IDS = setOf("vita3k", "vita3k-zx", "vita3k-plus")
    private val APP_FILES = Regex("""^(.*/Android/data/[^/]+/files)/VITA/ux0/user/\d{2}/savedata$""", RegexOption.IGNORE_CASE)
    private val ENTRY = Regex("""^([A-Za-z0-9_-]+):\s*(.*)$""")
    private val USER_ID = Regex("""^\d{2}$""")
    private const val CONFIG_FILE = "config.yml"
    private const val PREF_PATH = "pref-path"
    private const val USER = "user-id"
    private const val DEFAULT_USER = "00"

    /**
     * The save folder named by the Vita3K config beside the first of [candidates] that sits in a
     * Vita3K app files folder with a readable config. Null for a layout that is not Vita3K, and
     * when no config is readable or it names no path.
     */
    fun liveSaveData(config: SavePathConfig, candidates: List<String>, fal: FileAccessLayer): String? {
        if (config.emulatorId !in CONFIG_IDS) return null
        return candidates
            .mapNotNull { APP_FILES.find(it.trimEnd('/'))?.groupValues?.get(1) }
            .distinct()
            .firstNotNullOfOrNull { appFiles ->
                fal.readBytes("$appFiles/$CONFIG_FILE")?.let { saveDataOf(String(it, Charsets.UTF_8)) }
            }
    }

    fun saveDataOf(configYaml: String): String? {
        val entries = configYaml.lineSequence()
            .mapNotNull { ENTRY.find(it.trimEnd()) }
            .associate { it.groupValues[1] to unquote(it.groupValues[2]) }
        val prefPath = entries[PREF_PATH]?.trimEnd('/')?.takeIf { it.startsWith("/") } ?: return null
        val user = entries[USER]?.takeIf { USER_ID.matches(it) } ?: DEFAULT_USER
        return "$prefPath/ux0/user/$user/savedata"
    }

    private fun unquote(value: String): String {
        val trimmed = value.trim()
        val quoted = trimmed.length >= 2 &&
            (trimmed.first() == '"' || trimmed.first() == '\'') && trimmed.last() == trimmed.first()
        return if (quoted) trimmed.substring(1, trimmed.length - 1) else trimmed
    }
}

/**
 * The save folder an emulator that can relocate its data is writing to right now, for the
 * emulators that record where: standalone PPSSPP and Vita3K.
 */
object LiveSaveLocation {
    fun liveSaveData(config: SavePathConfig, candidates: List<String>, fal: FileAccessLayer): String? =
        PpssppMemoryStick.liveSaveData(config, candidates, fal)
            ?: Vita3kPrefPath.liveSaveData(config, candidates, fal)
}
