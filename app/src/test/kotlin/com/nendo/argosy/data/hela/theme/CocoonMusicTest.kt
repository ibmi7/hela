package com.nendo.argosy.data.hela.theme

import com.nendo.argosy.data.model.hela.CocoonThemeManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CocoonMusicTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun pack(vararg files: String): File {
        val dir = temp.newFolder("pack")
        files.forEach { relative ->
            File(dir, relative).apply { parentFile?.mkdirs() }.writeText("x")
        }
        return dir
    }

    private val backgroundMusicMetadata = """
        {
          "id": "3ds-background-music-3bd40c3c",
          "name": "3DS Background Music",
          "category": "music",
          "settings": {
            "music_mode": "PLAYLIST",
            "music_playback_mode": "IN ORDER",
            "music_playlist": "music/track.mp3",
            "music_time_schedule": "0:0|music/track.mp3||4:0|music/track.mp3"
          }
        }
    """.trimIndent()

    private val firstTimeSetupMetadata = """
        {
          "id": "nintendo-3ds-first-time-setup-8a25a4",
          "name": "Nintendo 3DS First Time Setup",
          "category": "music",
          "settings": {
            "music_mode": "PLAYLIST",
            "music_time_schedule": "",
            "music_playlist": "music/3DS_First_Time_Setup_-_Lost_Music.mp3"
          }
        }
    """.trimIndent()

    @Test
    fun `asset pack settings carry the playlist`() {
        val manifest = CocoonThemeParser.parse(backgroundMusicMetadata, "fallback")

        assertEquals("PLAYLIST", manifest.musicMode)
        assertEquals("music/track.mp3", manifest.musicPlaylist)
        assertEquals("music", manifest.category)
    }

    @Test
    fun `theme level playlist is read from the root`() {
        val manifest = CocoonThemeParser.parse(
            """{"name":"t","music_mode":"PLAYLIST","music_playlist":"music/a.mp3"}""",
            "fallback"
        )

        assertEquals("music/a.mp3", manifest.musicPlaylist)
    }

    @Test
    fun `single track pack yields its track`() {
        val dir = pack("music/track.mp3")

        val tracks = CocoonMusic.find(dir, CocoonThemeParser.parse(backgroundMusicMetadata, "f"))

        assertEquals(listOf("3DS Background Music"), tracks.map { it.title })
        assertTrue(tracks.single().path.endsWith("track.mp3"))
    }

    @Test
    fun `playlist entry matches a file whose case differs`() {
        val dir = pack("music/3ds_first_time_setup_-_lost_music.mp3")

        val tracks = CocoonMusic.find(dir, CocoonThemeParser.parse(firstTimeSetupMetadata, "f"))

        assertEquals(1, tracks.size)
        assertEquals("Nintendo 3DS First Time Setup", tracks.single().title)
        assertTrue(tracks.single().path.endsWith("3ds_first_time_setup_-_lost_music.mp3"))
    }

    @Test
    fun `listed tracks come first and the rest follow by name`() {
        val dir = pack("music/a.mp3", "music/b.ogg", "music/c.flac")
        val manifest = CocoonThemeManifest("n", musicPlaylist = "music/c.flac||music/a.mp3")

        assertEquals(listOf("c", "a", "b"), CocoonMusic.find(dir, manifest).map { it.title })
    }

    @Test
    fun `playlist entries that match no file are skipped`() {
        val dir = pack("music/a.mp3")
        val manifest = CocoonThemeManifest("n", musicPlaylist = "music/gone.mp3")

        assertEquals(listOf("n"), CocoonMusic.find(dir, manifest).map { it.title })
    }

    @Test
    fun `several tracks are titled from their file names`() {
        val dir = pack("music/night_theme.mp3", "music/Day  Theme.ogg")

        assertEquals(
            listOf("Day Theme", "night theme"),
            CocoonMusic.find(dir, CocoonThemeManifest("n")).map { it.title }
        )
    }

    @Test
    fun `non audio files are not tracks`() {
        val dir = pack("music/cover.png", "music/notes.txt")

        assertTrue(CocoonMusic.find(dir, CocoonThemeManifest("n")).isEmpty())
    }

    @Test
    fun `disabled music mode yields no tracks`() {
        val dir = pack("music/a.mp3")

        assertTrue(CocoonMusic.find(dir, CocoonThemeManifest("n", musicMode = "disabled")).isEmpty())
    }

    @Test
    fun `pack without a music folder has no tracks`() {
        assertTrue(CocoonMusic.find(pack("sounds/navigate.ogg"), CocoonThemeManifest("n")).isEmpty())
    }
}
