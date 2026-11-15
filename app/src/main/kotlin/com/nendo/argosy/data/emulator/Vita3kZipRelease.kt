package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.emulator.savepath.SavePathAuthority
import com.nendo.argosy.data.emulator.savepath.SavePathRequest
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.entity.GameEntity
import com.nendo.argosy.data.platform.PlatformDefinitions
import com.nendo.argosy.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Vita3kZipRelease @Inject constructor(
    private val gameDao: GameDao,
    private val emulatorResolver: EmulatorResolver,
    private val savePathAuthority: SavePathAuthority
) {
    data class Candidate(val game: GameEntity, val zip: File, val titleId: String)

    /**
     * The zip of [gameId] that can be replaced by a [Vita3kStub]: a PS Vita game launched with a
     * Vita3K build, pointing at an existing zip, whose title id is known and installed in Vita3K.
     */
    suspend fun candidate(gameId: Long): Candidate? = withContext(Dispatchers.IO) {
        val game = gameDao.getById(gameId) ?: return@withContext null
        if (PlatformDefinitions.getCanonicalSlug(game.platformSlug) != VITA) return@withContext null
        val zip = game.localPath?.let(::File)?.let(::zipOf)
            ?: return@withContext refuse(game, "no zip at ${game.localPath}")
        val emulator = emulatorResolver.resolveEmulatorForGame(game.id, game.platformId, game.platformSlug)
            ?.takeIf { it.launchConfig is LaunchConfig.Vita3K }
            ?: return@withContext refuse(game, "emulator is not Vita3K")
        val titleId = titleIdOf(game, zip) ?: return@withContext refuse(game, "unknown title id")
        val installed = isInstalled(game, emulator, titleId)
        if (installed != true) return@withContext refuse(game, "$titleId installed=$installed")
        Candidate(game, zip, titleId)
    }

    /**
     * Replaces the zip of [gameId] by its stub, keeping the game downloaded and its save sync
     * state untouched. False when the game is not a [candidate] or the stub cannot be written.
     */
    suspend fun release(gameId: Long): Boolean = withContext(Dispatchers.IO) {
        val candidate = candidate(gameId) ?: return@withContext false
        val (game, zip, titleId) = candidate
        val stub = Vita3kStub.stubFor(zip)
        val written = runCatching { Vita3kStub.write(stub, titleId) }
            .onFailure { Logger.warn(TAG, "release: cannot write ${stub.name}", it) }
            .isSuccess
        if (!written || Vita3kStub.read(stub) != titleId) return@withContext false
        if (game.titleId.isNullOrBlank() || game.saveId.isNullOrBlank()) {
            gameDao.setTitleAndSaveIdWithLock(
                game.id,
                game.titleId?.takeIf { it.isNotBlank() } ?: titleId,
                game.saveId?.takeIf { it.isNotBlank() } ?: titleId,
                locked = true
            )
        }
        gameDao.updateLocalPath(game.id, stub.absolutePath, game.source, game.fileOrigin, game.addedAt)
        val deleted = zip.delete()
        Logger.info(TAG, "release: ${game.title} $titleId -> ${stub.name}, zip deleted=$deleted")
        deleted
    }

    private fun zipOf(local: File): File? {
        val zip = if (Vita3kStub.isStub(local)) File(local.parentFile, "${local.nameWithoutExtension}.$ZIP") else local
        return zip.takeIf { it.isFile && it.extension.equals(ZIP, ignoreCase = true) }
    }

    private fun refuse(game: GameEntity, reason: String): Candidate? {
        Logger.debug(TAG, "candidate: ${game.title} refused, $reason")
        return null
    }

    private fun titleIdOf(game: GameEntity, zip: File): String? =
        game.saveId?.takeIf { TITLE_ID.matches(it) }
            ?: game.titleId?.takeIf { TITLE_ID.matches(it) }
            ?: BRACKETED.find(zip.nameWithoutExtension)?.groupValues?.get(1)
            ?: VitaPkgHeader.titleIdResultOf(zip, game.platformSlug)?.titleId

    private suspend fun isInstalled(game: GameEntity, emulator: EmulatorDef, titleId: String): Boolean? {
        val saveData = savePathAuthority.resolve(
            SavePathRequest(
                platformSlug = game.platformSlug,
                emulatorId = emulator.id,
                emulatorPackage = emulator.packageName,
                platformId = game.platformId
            )
        ).basePath
        Logger.debug(TAG, "isInstalled: $titleId save data=$saveData")
        return Vita3kInstalledApps.isInstalled(saveData, titleId) { dir -> File(dir).list()?.toList() }
    }

    private companion object {
        const val TAG = "Vita3kZipRelease"
        const val VITA = "vita"
        const val ZIP = "zip"
        val TITLE_ID = Regex("""^[A-Z]{4}\d{5}$""")
        val BRACKETED = Regex("""\[([A-Z]{4}\d{5})]""")
    }
}
