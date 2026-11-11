package com.nendo.argosy.data.hela.theme

import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

sealed interface PackImportResult {
    data class Success(val packDir: File) : PackImportResult
    data class Failure(val reason: PackImportFailure) : PackImportResult
}

enum class PackImportFailure { NOT_A_ZIP, NO_THEME, UNSAFE_ENTRY, TOO_LARGE, IO }

object CocoonPackImporter {

    const val THEME_FILE = "theme.json"
    const val ASSET_FILE = "metadata.json"
    private val MANIFEST_FILES = listOf(THEME_FILE, ASSET_FILE)
    private val CONTENT_DIRS = listOf(
        CocoonWallpaperResolver.WALLPAPERS_DIR,
        CocoonSounds.SOUNDS_DIR,
        CocoonMusic.MUSIC_DIR
    )
    private const val MAX_TOTAL_BYTES = 1024L * 1024L * 1024L
    private const val MAX_ENTRIES = 5000
    private const val STAGING_PREFIX = ".staging-"
    private val UNSAFE_ID_CHARS = Regex("[^A-Za-z0-9._-]")

    /**
     * Extracts the pack folder of [zip] into [themesDir]/<id>, replacing a pack of the same id.
     * The pack folder is the parent of the shallowest theme.json or metadata.json, or of the
     * shallowest wallpapers/, sounds/ or music/ folder in an archive without one.
     */
    fun import(zip: File, themesDir: File): PackImportResult {
        val archive = try {
            ZipFile(zip)
        } catch (e: ZipException) {
            return PackImportResult.Failure(PackImportFailure.NOT_A_ZIP)
        } catch (e: IOException) {
            return PackImportResult.Failure(PackImportFailure.IO)
        }
        return archive.use { extract(it, zip, themesDir) }
    }

    private fun extract(archive: ZipFile, zip: File, themesDir: File): PackImportResult {
        val entries = archive.entries().asSequence().filter { !it.isDirectory }.toList()
        if (entries.size > MAX_ENTRIES) return PackImportResult.Failure(PackImportFailure.TOO_LARGE)
        val root = themeRoot(entries) ?: return PackImportResult.Failure(PackImportFailure.NO_THEME)
        val members = entries.filter { it.name.startsWith(root) }
        if (members.sumOf { it.size.coerceAtLeast(0L) } > MAX_TOTAL_BYTES) {
            return PackImportResult.Failure(PackImportFailure.TOO_LARGE)
        }
        val id = packId(root, zip)
        val staging = File(themesDir, STAGING_PREFIX + id)
        val target = File(themesDir, id)
        return try {
            staging.deleteRecursively()
            staging.mkdirs()
            val stagingRoot = staging.canonicalPath + File.separator
            var written = 0L
            for (entry in members) {
                val destination = File(staging, entry.name.removePrefix(root))
                if (!destination.canonicalPath.startsWith(stagingRoot)) {
                    staging.deleteRecursively()
                    return PackImportResult.Failure(PackImportFailure.UNSAFE_ENTRY)
                }
                destination.parentFile?.mkdirs()
                written += copyEntry(archive, entry, destination, MAX_TOTAL_BYTES - written)
                if (written > MAX_TOTAL_BYTES) {
                    staging.deleteRecursively()
                    return PackImportResult.Failure(PackImportFailure.TOO_LARGE)
                }
            }
            target.deleteRecursively()
            if (!staging.renameTo(target)) {
                staging.deleteRecursively()
                return PackImportResult.Failure(PackImportFailure.IO)
            }
            PackImportResult.Success(target)
        } catch (e: IOException) {
            staging.deleteRecursively()
            PackImportResult.Failure(PackImportFailure.IO)
        }
    }

    private fun copyEntry(archive: ZipFile, entry: ZipEntry, destination: File, budget: Long): Long {
        var copied = 0L
        archive.getInputStream(entry).use { input ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (copied <= budget) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copied += read
                }
            }
        }
        return copied
    }

    private fun themeRoot(entries: List<ZipEntry>): String? {
        val names = entries.map { it.name.replace('\\', '/') }
        val manifest = names
            .filter { name -> MANIFEST_FILES.any { name == it || name.endsWith("/$it") } }
            .minByOrNull { depth(it) }
        if (manifest != null) return manifest.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }
        return names
            .mapNotNull { name ->
                CONTENT_DIRS.firstNotNullOfOrNull { dir ->
                    val marker = "$dir/"
                    when {
                        name.startsWith(marker) -> ""
                        name.contains("/$marker") -> name.substringBefore("/$marker") + "/"
                        else -> null
                    }
                }
            }
            .minByOrNull { depth(it) }
    }

    private fun depth(path: String): Int = path.count { it == '/' }

    private fun packId(root: String, zip: File): String {
        val folder = root.trimEnd('/').substringAfterLast('/')
        val raw = folder.ifEmpty { zip.nameWithoutExtension }
        return raw.replace(UNSAFE_ID_CHARS, "_").trim('.').ifEmpty { "pack" }
    }
}
