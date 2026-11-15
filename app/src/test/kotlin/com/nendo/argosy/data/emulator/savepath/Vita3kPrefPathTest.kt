package com.nendo.argosy.data.emulator.savepath

import com.nendo.argosy.data.emulator.SavePathRegistry
import com.nendo.argosy.data.storage.FileAccessLayer
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Vita3kPrefPathTest {

    private val fal: FileAccessLayer = mockk(relaxed = true)
    private val vita3k = SavePathRegistry.getConfig("vita3k")!!
    private val ppsspp = SavePathRegistry.getConfig("ppsspp")!!

    private fun config(yaml: String) {
        every { fal.readBytes("$APP_FILES/config.yml") } returns yaml.toByteArray()
    }

    @Test
    fun `a moved VitaFS names the save folder under its pref path`() {
        config("---\nlog-level: 0\npref-path: /storage/4A21-0000/Vita3k/\nuser-id: 00\nuser-lang: \"\"\n")

        assertEquals(
            "/storage/4A21-0000/Vita3k/ux0/user/00/savedata",
            Vita3kPrefPath.liveSaveData(vita3k, listOf(REGISTERED), fal)
        )
    }

    @Test
    fun `the evaluated folder ahead of the registered one does not hide the config`() {
        config("pref-path: /storage/4A21-0000/Vita3k\n")

        assertEquals(
            "/storage/4A21-0000/Vita3k/ux0/user/00/savedata",
            LiveSaveLocation.liveSaveData(vita3k, listOf("/storage/old/Vita3k/ux0/user/00/savedata", REGISTERED), fal)
        )
    }

    @Test
    fun `quotes are dropped and another user id is followed`() {
        assertEquals(
            "/sdcard/vita/ux0/user/01/savedata",
            Vita3kPrefPath.saveDataOf("pref-path: \"/sdcard/vita/\"\nuser-id: '01'\n")
        )
    }

    @Test
    fun `a config without a usable pref path names nothing`() {
        assertNull(Vita3kPrefPath.saveDataOf("pref-path: \"\"\nuser-id: 00\n"))
        assertNull(Vita3kPrefPath.saveDataOf("pref-path: relative/dir\n"))
        assertNull(Vita3kPrefPath.saveDataOf("log-level: 0\n"))
        assertEquals("/a/ux0/user/00/savedata", Vita3kPrefPath.saveDataOf("pref-path: /a\nuser-id: abc\n"))
    }

    @Test
    fun `an unreadable config and another emulator name nothing`() {
        every { fal.readBytes(any()) } returns null
        assertNull(Vita3kPrefPath.liveSaveData(vita3k, listOf(REGISTERED), fal))

        config("pref-path: /storage/4A21-0000/Vita3k/\n")
        assertNull(Vita3kPrefPath.liveSaveData(ppsspp, listOf(REGISTERED), fal))
        assertNull(Vita3kPrefPath.liveSaveData(vita3k, listOf("/storage/4A21-0000/Vita3k/ux0/user/00/savedata"), fal))
    }

    private companion object {
        const val APP_FILES = "/storage/emulated/0/Android/data/org.vita3k.emulator/files"
        const val REGISTERED = "$APP_FILES/VITA/ux0/user/00/savedata"
    }
}
