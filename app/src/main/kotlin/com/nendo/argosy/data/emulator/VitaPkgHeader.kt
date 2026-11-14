package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.platform.PlatformDefinitions
import java.io.File
import java.util.zip.ZipFile

object VitaPkgHeader {
    const val CONTENT_ID_OFFSET = 0x30
    const val CONTENT_ID_LENGTH = 0x24
    const val HEADER_BYTES = CONTENT_ID_OFFSET + CONTENT_ID_LENGTH

    private val MAGIC = byteArrayOf(0x7F, 0x50, 0x4B, 0x47)
    private val CONTENT_ID = Regex("""^[A-Z]{2}\d{4}-([A-Z]{4}\d{5})_\d{2}-[A-Z0-9]{16}$""")

    fun titleIdOf(header: ByteArray): String? {
        if (header.size < HEADER_BYTES) return null
        if (MAGIC.indices.any { header[it] != MAGIC[it] }) return null
        val contentId = String(
            header,
            CONTENT_ID_OFFSET,
            CONTENT_ID_LENGTH,
            Charsets.US_ASCII
        )
        return CONTENT_ID.find(contentId)?.groupValues?.get(1)
    }

    fun titleIdIn(zip: ZipFile): String? {
        val pkgEntry = zip.entries().asSequence()
            .firstOrNull { !it.isDirectory && it.name.endsWith(".pkg", ignoreCase = true) }
            ?: return null
        val header = ByteArray(HEADER_BYTES)
        var read = 0
        zip.getInputStream(pkgEntry).use { input ->
            while (read < header.size) {
                val count = input.read(header, read, header.size - read)
                if (count < 0) break
                read += count
            }
        }
        return if (read < header.size) null else titleIdOf(header)
    }

    fun titleIdResultOf(romFile: File, platformSlug: String): TitleIdResult? {
        if (PlatformDefinitions.getCanonicalSlug(platformSlug) != "vita") return null
        if (!romFile.extension.equals("zip", ignoreCase = true) || !romFile.isFile) return null
        val titleId = runCatching { ZipFile(romFile).use { titleIdIn(it) } }.getOrNull() ?: return null
        return TitleIdResult(titleId = titleId, fromBinary = true)
    }
}
