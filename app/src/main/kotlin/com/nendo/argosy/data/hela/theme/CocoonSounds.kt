package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.core.input.SoundConfig
import com.nendo.argosy.core.input.SoundType
import java.io.File

object CocoonSounds {

    const val SOUNDS_DIR = "sounds"

    val NAMES: Set<String> = setOf(
        "navigate", "select", "back", "folder_open", "folder_close", "launch", "error",
        "notification", "discord_open", "discord_close", "screen_swap", "grid_zoom_in",
        "grid_zoom_out", "reorder_pickup", "reorder_place", "open_edit", "pop",
        "slider_increase", "slider_decrease", "saving", "saving_end", "retroachievements"
    )

    private val AUDIO_EXTENSIONS = setOf("wav", "mp3", "ogg", "m4a", "flac")

    private val TYPES_BY_NAME: Map<String, List<SoundType>> = mapOf(
        "navigate" to listOf(SoundType.NAVIGATE),
        "select" to listOf(SoundType.SELECT),
        "back" to listOf(SoundType.BACK),
        "folder_open" to listOf(SoundType.OPEN_MODAL),
        "folder_close" to listOf(SoundType.CLOSE_MODAL),
        "launch" to listOf(SoundType.LAUNCH_GAME),
        "error" to listOf(SoundType.ERROR, SoundType.BOUNDARY),
        "notification" to listOf(SoundType.DOWNLOAD_COMPLETE),
        "screen_swap" to listOf(SoundType.SECTION_CHANGE),
        "pop" to listOf(SoundType.TOGGLE),
        "slider_increase" to listOf(SoundType.VOLUME_PREVIEW)
    )

    /**
     * Cocoon sound name to file path for every recognised sound under [packDir]/sounds. Files
     * one folder deeper are accepted for packs that nest their sounds under a pack folder.
     */
    fun find(packDir: File): Map<String, String> {
        val soundsDir = File(packDir, SOUNDS_DIR)
        val direct = soundsDir.listFiles().orEmpty().sortedBy { it.name }
        val nested = direct.filter { it.isDirectory }
            .flatMap { dir -> dir.listFiles().orEmpty().sortedBy { it.name } }
        val found = LinkedHashMap<String, String>()
        for (file in direct + nested) {
            if (!file.isFile || file.extension.lowercase() !in AUDIO_EXTENSIONS) continue
            val name = file.nameWithoutExtension.lowercase()
            if (name in NAMES && name !in found) found[name] = file.absolutePath
        }
        return found
    }

    /**
     * Argosy sound events served by a pack. Cocoon sounds with no Argosy event are left out.
     */
    fun configs(sounds: Map<String, String>): Map<SoundType, SoundConfig> {
        val configs = LinkedHashMap<SoundType, SoundConfig>()
        for ((name, path) in sounds) {
            TYPES_BY_NAME[name].orEmpty().forEach { type ->
                configs[type] = SoundConfig(customFilePath = path)
            }
        }
        return configs
    }
}
