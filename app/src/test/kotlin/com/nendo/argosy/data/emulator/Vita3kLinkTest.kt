package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.emulator.savepath.SavePathAuthority
import com.nendo.argosy.data.emulator.savepath.SavePathResolution
import com.nendo.argosy.data.emulator.savepath.SavePathSource
import com.nendo.argosy.data.emulator.savepath.SavePathVerdict
import com.nendo.argosy.data.local.dao.DownloadQueueDao
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.dao.GameFileDao
import com.nendo.argosy.data.local.entity.DownloadQueueEntity
import com.nendo.argosy.data.local.entity.GameEntity
import com.nendo.argosy.data.local.entity.GameFileEntity
import com.nendo.argosy.data.model.FileOrigin
import com.nendo.argosy.data.model.GameSource
import com.nendo.argosy.data.platform.PlatformFolderResolver
import com.nendo.argosy.data.preferences.SyncPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class Vita3kLinkTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val gameDao: GameDao = mockk(relaxed = true)
    private val gameFileDao: GameFileDao = mockk()
    private val downloadQueueDao: DownloadQueueDao = mockk()
    private val emulatorResolver: EmulatorResolver = mockk()
    private val savePathAuthority: SavePathAuthority = mockk()
    private val folderResolver: PlatformFolderResolver = mockk()
    private val syncPrefs: SyncPreferencesRepository = mockk()
    private val link = Vita3kLink(gameDao, gameFileDao, downloadQueueDao, emulatorResolver, savePathAuthority, folderResolver, syncPrefs)
    private val vita3k = EmulatorRegistry.getById("vita3k")!!

    private lateinit var library: File

    @Before
    fun setUp() {
        val vitaFs = temp.newFolder("Vita3k")
        File(vitaFs, "ux0/app/PCSB00245").mkdirs()
        File(vitaFs, "ux0/user/00/savedata").mkdirs()
        library = temp.newFolder("library", "psvita")
        coEvery { emulatorResolver.resolveEmulatorForGame(any(), any(), any()) } returns vita3k
        coEvery { savePathAuthority.resolve(any()) } returns SavePathResolution(
            config = null,
            configId = "vita3k",
            basePath = File(vitaFs, "ux0/user/00/savedata").absolutePath,
            unresolvedShape = null,
            source = SavePathSource.EVALUATED_DEFAULT,
            verdict = SavePathVerdict.Ok
        )
        coEvery { folderResolver.downloadDirForPlatform(5L, "psvita") } returns library
        coEvery { downloadQueueDao.getRecoverableDownloads() } returns emptyList()
        coEvery { syncPrefs.getRommUserId() } returns 1L
        coEvery { gameDao.getByPlatform(5L, 1L) } returns emptyList()
        coEvery { gameFileDao.getFilesForGame(226L) } returns listOf(mockk<GameFileEntity>(), mockk())
    }

    private fun persona(localPath: String? = null) = mockk<GameEntity>(relaxed = true) {
        every { id } returns 226L
        every { title } returns "Persona 4 Golden"
        every { platformSlug } returns "psvita"
        every { platformId } returns 5L
        every { rommId } returns 504L
        every { rommFileName } returns "Persona 4 Golden"
        every { this@mockk.localPath } returns localPath
        every { titleId } returns null
        every { saveId } returns null
    }.also { coEvery { gameDao.getById(226L) } returns it }

    @Test
    fun `a linked game gets a flat stub where its zip would land`() = runTest {
        persona()

        assertTrue(link.link(226L, "PCSB00245"))

        val stub = File(library, "Persona 4 Golden.vita3k")
        assertEquals("PCSB00245", Vita3kStub.read(stub))
        coVerify { gameDao.setTitleAndSaveIdWithLock(226L, "PCSB00245", "PCSB00245", true) }
        coVerify { gameDao.updateLocalPath(226L, stub.absolutePath, GameSource.ROMM_SYNCED, FileOrigin.ADOPTED, any()) }
    }

    @Test
    fun `a game already local is not linked`() = runTest {
        persona(localPath = "/somewhere/Persona 4 Golden.zip")

        assertFalse(link.link(226L, "PCSB00245"))
        coVerify(exactly = 0) { gameDao.updateLocalPath(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a queued game is not linked`() = runTest {
        persona()
        coEvery { downloadQueueDao.getRecoverableDownloads() } returns listOf(mockk<DownloadQueueEntity> { every { gameId } returns 226L })

        assertFalse(link.link(226L, "PCSB00245"))
        assertFalse(File(library, "Persona 4 Golden.vita3k").exists())
    }

    @Test
    fun `an app missing from ux0 app is not linked`() = runTest {
        persona()

        assertFalse(link.link(226L, "PCSE00261"))
    }

    @Test
    fun `a different file at the stub path is left alone`() = runTest {
        persona()
        val other = File(library, "Persona 4 Golden.vita3k").apply { writeText("PCSB99999") }

        assertFalse(link.link(226L, "PCSB00245"))
        assertEquals("PCSB99999", other.readText())
    }

    @Test
    fun `an id another local game holds is not linked twice`() = runTest {
        persona()
        val holder = mockk<GameEntity>(relaxed = true) {
            every { id } returns 999L
            every { localPath } returns "/x/P4G.zip"
            every { titleId } returns "PCSB00245"
            every { saveId } returns null
        }
        coEvery { gameDao.getByPlatform(5L, 1L) } returns listOf(holder)

        assertFalse(link.link(226L, "PCSB00245"))
    }
}
