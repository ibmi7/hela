package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.emulator.savepath.SavePathAuthority
import com.nendo.argosy.data.emulator.savepath.SavePathResolution
import com.nendo.argosy.data.emulator.savepath.SavePathSource
import com.nendo.argosy.data.emulator.savepath.SavePathVerdict
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.entity.GameEntity
import com.nendo.argosy.data.model.FileOrigin
import com.nendo.argosy.data.model.GameSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class Vita3kZipReleaseTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val gameDao: GameDao = mockk(relaxed = true)
    private val emulatorResolver: EmulatorResolver = mockk()
    private val savePathAuthority: SavePathAuthority = mockk()
    private val release = Vita3kZipRelease(gameDao, emulatorResolver, savePathAuthority)
    private val vita3k = EmulatorRegistry.getById("vita3k")!!

    private lateinit var zip: File
    private lateinit var vitaFs: File

    @Before
    fun setUp() {
        vitaFs = temp.newFolder("Vita3k")
        File(vitaFs, "ux0/app/PCSF00243").mkdirs()
        File(vitaFs, "ux0/user/00/savedata").mkdirs()
        val gameDir = temp.newFolder("library", "Killzone")
        zip = File(gameDir, "Killzone - Mercenary (Europe).zip").apply { writeBytes(ByteArray(64)) }
        coEvery { emulatorResolver.resolveEmulatorForGame(any(), any(), any()) } returns vita3k
        coEvery { savePathAuthority.resolve(any()) } returns SavePathResolution(
            config = null,
            configId = "vita3k",
            basePath = File(vitaFs, "ux0/user/00/savedata").absolutePath,
            unresolvedShape = null,
            source = SavePathSource.EVALUATED_DEFAULT,
            verdict = SavePathVerdict.Ok
        )
    }

    private fun game(
        localPath: String? = zip.absolutePath,
        titleId: String? = null,
        saveId: String? = "PCSF00243",
        platformSlug: String = "psvita"
    ) = mockk<GameEntity>(relaxed = true) {
        every { id } returns 222L
        every { title } returns "Killzone: Mercenary"
        every { this@mockk.platformSlug } returns platformSlug
        every { platformId } returns 5L
        every { this@mockk.localPath } returns localPath
        every { this@mockk.titleId } returns titleId
        every { this@mockk.saveId } returns saveId
        every { source } returns GameSource.ROMM_SYNCED
        every { fileOrigin } returns FileOrigin.ROMM_DOWNLOAD
    }.also { coEvery { gameDao.getById(222L) } returns it }

    @Test
    fun `an installed Vita game swaps its zip for a stub and keeps its save state`() = runTest {
        game()

        assertTrue(release.release(222L))

        val stub = File(zip.parentFile, "Killzone - Mercenary (Europe).vita3k")
        assertFalse(zip.exists())
        assertEquals("PCSF00243", Vita3kStub.read(stub))
        coVerify { gameDao.updateLocalPath(222L, stub.absolutePath, GameSource.ROMM_SYNCED, FileOrigin.ROMM_DOWNLOAD, any()) }
        coVerify { gameDao.setTitleAndSaveIdWithLock(222L, "PCSF00243", "PCSF00243", true) }
        coVerify(exactly = 0) { gameDao.clearLocalPath(any()) }
    }

    @Test
    fun `a game not installed in Vita3K is not a candidate`() = runTest {
        game(saveId = "PCSB00456")

        assertNull(release.candidate(222L))
        assertFalse(release.release(222L))
        assertTrue(zip.exists())
    }

    @Test
    fun `other platforms, stubs and missing files are never candidates`() = runTest {
        game(platformSlug = "psp")
        assertNull(release.candidate(222L))

        val lone = File(zip.parentFile, "x.vita3k").apply { writeText("PCSF00243") }
        game(localPath = lone.absolutePath)
        assertNull(release.candidate(222L))

        game(localPath = File(zip.parentFile, "gone.zip").absolutePath)
        assertNull(release.candidate(222L))
    }

    @Test
    fun `another emulator for the platform is not a candidate`() = runTest {
        game()
        coEvery { emulatorResolver.resolveEmulatorForGame(any(), any(), any()) } returns EmulatorRegistry.getById("ppsspp")!!

        assertNull(release.candidate(222L))
    }

    @Test
    fun `an unknown title id is not a candidate`() = runTest {
        game(saveId = null, titleId = null)

        assertNull(release.candidate(222L))
    }

    @Test
    fun `a zip put back beside its stub is released again`() = runTest {
        val stub = Vita3kStub.stubFor(zip).apply { writeText("PCSF00243") }
        game(localPath = stub.absolutePath)

        assertEquals(zip, release.candidate(222L)?.zip)
        assertTrue(release.release(222L))
        assertFalse(zip.exists())
        assertEquals("PCSF00243", Vita3kStub.read(stub))
    }
}
