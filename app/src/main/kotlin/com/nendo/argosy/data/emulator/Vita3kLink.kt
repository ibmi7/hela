package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.emulator.Vita3kLinkMatcher.InstalledApp
import com.nendo.argosy.data.emulator.Vita3kLinkMatcher.LinkGame
import com.nendo.argosy.data.emulator.Vita3kLinkMatcher.Match
import com.nendo.argosy.data.emulator.savepath.SavePathAuthority
import com.nendo.argosy.data.emulator.savepath.SavePathRequest
import com.nendo.argosy.data.local.dao.DownloadQueueDao
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.dao.GameFileDao
import com.nendo.argosy.data.local.entity.GameEntity
import com.nendo.argosy.data.model.FileOrigin
import com.nendo.argosy.data.model.GameSource
import com.nendo.argosy.data.platform.PlatformDefinitions
import com.nendo.argosy.data.platform.PlatformFolderResolver
import com.nendo.argosy.data.preferences.SyncPreferencesRepository
import com.nendo.argosy.util.FileNames
import com.nendo.argosy.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Vita3kLink @Inject constructor(
    private val gameDao: GameDao,
    private val gameFileDao: GameFileDao,
    private val downloadQueueDao: DownloadQueueDao,
    private val emulatorResolver: EmulatorResolver,
    private val savePathAuthority: SavePathAuthority,
    private val platformFolderResolver: PlatformFolderResolver,
    private val syncPreferencesRepository: SyncPreferencesRepository
) {
    sealed interface Scan {
        data object Unreadable : Scan
        data class Found(val sure: List<Match>, val byTitle: List<Match>) : Scan
    }

    /**
     * The never-downloaded PS Vita games of [platformId] that can be linked to an app installed
     * in Vita3K, split into sure pairs and pairs resting on the title alone. Writes nothing.
     */
    suspend fun scan(platformId: Long): Scan = withContext(Dispatchers.IO) {
        val games = gameDao.getByPlatform(platformId, syncPreferencesRepository.getRommUserId())
            .filter { PlatformDefinitions.getCanonicalSlug(it.platformSlug) == VITA }
        val held = games.filter { it.localPath != null }.flatMap(::heldIds).toSet()
        val queued = downloadQueueDao.getRecoverableDownloads().map { it.gameId }.toSet()
        val candidates = games.filter { it.localPath == null && it.rommId != null && it.id !in queued }
        val byAppsDir = candidates.groupBy { appsDirFor(it) }
        val readable = byAppsDir.keys.filterNotNull().associateWith(::installedApps).filterValues { it != null }
        if (candidates.isNotEmpty() && readable.isEmpty()) {
            Logger.info(TAG, "scan: no readable ux0/app among ${byAppsDir.keys}")
            return@withContext Scan.Unreadable
        }
        val results = readable.map { (dir, apps) ->
            val linkGames = byAppsDir[dir].orEmpty().map { linkGameOf(it) }
            Vita3kLinkMatcher.match(linkGames, apps.orEmpty(), held)
        }
        val found = Scan.Found(results.flatMap { it.sure }, results.flatMap { it.byTitle })
        Logger.info(TAG, "scan: ${candidates.size} candidates, apps in ${readable.keys}")
        found.sure.forEach { Logger.info(TAG, "scan: sure ${it.gameTitle} -> ${it.titleId} (${it.appTitle})") }
        found.byTitle.forEach { Logger.info(TAG, "scan: title ${it.gameTitle} -> ${it.titleId} (${it.appTitle})") }
        found
    }

    /**
     * Points the never-downloaded game [gameId] at a new [Vita3kStub] for [titleId], written
     * where a Hela download would put the game's zip. False when the game is local, queued or
     * not launched with Vita3K, the app is not installed, another game holds [titleId], or a
     * different file sits at the stub path.
     */
    suspend fun link(gameId: Long, titleId: String): Boolean = withContext(Dispatchers.IO) {
        val game = gameDao.getById(gameId) ?: return@withContext false
        val id = titleId.uppercase()
        if (PlatformDefinitions.getCanonicalSlug(game.platformSlug) != VITA) return@withContext refuse(game, "not vita")
        if (game.localPath != null || game.rommId == null) return@withContext refuse(game, "already local or not romm")
        if (downloadQueueDao.getRecoverableDownloads().any { it.gameId == gameId }) return@withContext refuse(game, "queued")
        val appsDir = appsDirFor(game) ?: return@withContext refuse(game, "emulator is not Vita3K")
        if (installedApps(appsDir)?.any { it.titleId == id } != true) return@withContext refuse(game, "$id not installed")
        val holders = gameDao.getByPlatform(game.platformId, syncPreferencesRepository.getRommUserId())
            .filter { it.id != gameId && it.localPath != null && id in heldIds(it) }
        if (holders.isNotEmpty()) return@withContext refuse(game, "$id held by ${holders.map { it.title }}")
        val stub = stubFor(game)
        if (stub.exists() && Vita3kStub.read(stub) != id) return@withContext refuse(game, "${stub.path} in the way")
        val written = runCatching {
            stub.parentFile?.mkdirs()
            Vita3kStub.write(stub, id)
        }.onFailure { Logger.warn(TAG, "link: cannot write ${stub.path}", it) }.isSuccess
        if (!written || Vita3kStub.read(stub) != id) return@withContext false
        if (game.titleId.isNullOrBlank() || game.saveId.isNullOrBlank()) {
            gameDao.setTitleAndSaveIdWithLock(
                game.id,
                game.titleId?.takeIf { it.isNotBlank() } ?: id,
                game.saveId?.takeIf { it.isNotBlank() } ?: id,
                locked = true
            )
        }
        gameDao.updateLocalPath(game.id, stub.absolutePath, GameSource.ROMM_SYNCED, FileOrigin.ADOPTED)
        Logger.info(TAG, "link: ${game.title} $id -> ${stub.path}")
        true
    }

    private suspend fun stubFor(game: GameEntity): File {
        val dir = platformFolderResolver.downloadDirForPlatform(game.platformId, game.platformSlug)
        val fileCount = gameFileDao.getFilesForGame(game.id).size
        return File(dir, Vita3kStub.stubFor(File(downloadZipName(game.rommFileName, game.title, fileCount))).name)
    }

    private suspend fun linkGameOf(game: GameEntity) = LinkGame(
        gameId = game.id,
        title = game.title,
        titleId = game.titleId,
        saveId = game.saveId,
        rommFileName = game.rommFileName,
        fileNames = gameFileDao.getFilesForGame(game.id).map { it.fileName }
    )

    private suspend fun appsDirFor(game: GameEntity): String? {
        val emulator = emulatorResolver.resolveEmulatorForGame(game.id, game.platformId, game.platformSlug)
            ?.takeIf { it.launchConfig is LaunchConfig.Vita3K }
            ?: return null
        val saveData = savePathAuthority.resolve(
            SavePathRequest(
                platformSlug = game.platformSlug,
                emulatorId = emulator.id,
                emulatorPackage = emulator.packageName,
                platformId = game.platformId
            )
        ).basePath
        return saveData?.let(Vita3kInstalledApps::appsDirOf)
    }

    private fun installedApps(appsDir: String): List<InstalledApp>? {
        val names = File(appsDir).list() ?: return null
        return names
            .filter { !it.endsWith(DECRYPTED_SUFFIX) && TITLE_ID.matches(it.uppercase()) }
            .map { name ->
                val sfo = File(appsDir, "$name/$PARAM_SFO")
                val info = runCatching { sfo.readBytes() }.getOrNull()?.let(VitaParamSfo::parse)
                InstalledApp(name.uppercase(), info?.title)
            }
    }

    private fun heldIds(game: GameEntity): List<String> =
        listOfNotNull(game.titleId, game.saveId).map { it.trim().uppercase() }.filter { TITLE_ID.matches(it) }

    private fun refuse(game: GameEntity, reason: String): Boolean {
        Logger.info(TAG, "link: ${game.title} refused, $reason")
        return false
    }

    companion object {
        private const val TAG = "Vita3kLink"
        private const val VITA = "vita"
        private const val DECRYPTED_SUFFIX = "_dec"
        private const val PARAM_SFO = "sce_sys/param.sfo"
        private val TITLE_ID = Regex("""^[A-Z]{4}\d{5}$""")

        /**
         * The file name a Hela download gives the zip of a RomM rom: `fs_name` itself for a
         * single file, `fs_name.zip` when RomM serves several files as one archive, folded by
         * [FileNames.sanitize] as the download queue folds it.
         */
        fun downloadZipName(rommFileName: String?, title: String, fileCount: Int): String {
            val base = rommFileName?.takeIf { it.isNotBlank() } ?: title
            val name = if (fileCount <= 1 && base.contains('.')) base else "$base.zip"
            return FileNames.sanitize(name)
        }
    }
}
