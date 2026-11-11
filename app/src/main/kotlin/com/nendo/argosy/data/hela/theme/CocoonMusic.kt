package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.data.model.hela.CocoonThemeManifest
import com.nendo.argosy.data.model.hela.PackTrack
import java.io.File

object CocoonMusic {

    const val MUSIC_DIR = "music"
    const val MODE_DISABLED = "DISABLED"

    private val AUDIO_EXTENSIONS = setOf("mp3", "ogg", "wav", "m4a", "flac", "opus")
    private val PLAYLIST_SEPARATORS = Regex("""\|\||[\n;,]""")

    /**
     * Tracks of a pack in playback order: the files named by music_playlist first, in that
     * order, then every other audio file under [packDir]/music by name. Playlist entries are
     * matched without regard to case. A pack with a single track titles it with the pack name.
     * Empty when the pack sets music_mode to DISABLED.
     */
    fun find(packDir: File, manifest: CocoonThemeManifest): List<PackTrack> {
        if (manifest.musicMode?.trim()?.uppercase() == MODE_DISABLED) return emptyList()
        val files = File(packDir, MUSIC_DIR).listFiles().orEmpty()
            .filter { it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS }
            .sortedBy { it.name.lowercase() }
        if (files.isEmpty()) return emptyList()
        val byName = files.associateBy { it.name.lowercase() }
        val listed = manifest.musicPlaylist.orEmpty()
            .split(PLAYLIST_SEPARATORS)
            .map { it.trim().replace('\\', '/').substringAfterLast('/').lowercase() }
            .filter { it.isNotEmpty() }
            .mapNotNull { byName[it] }
            .distinct()
        val ordered = listed + files.filterNot { it in listed }
        return ordered.map { file ->
            PackTrack(
                title = if (ordered.size == 1) manifest.name else title(file),
                path = file.absolutePath
            )
        }
    }

    private fun title(file: File): String =
        file.nameWithoutExtension.replace('_', ' ').replace(Regex("\\s+"), " ").trim()
}
