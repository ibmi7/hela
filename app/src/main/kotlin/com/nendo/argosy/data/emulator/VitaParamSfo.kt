package com.nendo.argosy.data.emulator

import java.nio.ByteBuffer
import java.nio.ByteOrder

object VitaParamSfo {
    data class Info(val titleId: String?, val title: String?)

    private val MAGIC = byteArrayOf(0x00, 0x50, 0x53, 0x46)
    private const val HEADER_BYTES = 0x14
    private const val ENTRY_BYTES = 0x10
    private const val FORMAT_UTF8 = 0x0204
    private const val FORMAT_UTF8_SPECIAL = 0x0004
    private const val KEY_TITLE = "TITLE"
    private const val KEY_TITLE_ID = "TITLE_ID"

    /**
     * The `TITLE_ID` and `TITLE` strings of a PS Vita `param.sfo`, laid out as psdevwiki
     * documents it. Null when [bytes] is not an SFO or its tables point outside the buffer.
     */
    fun parse(bytes: ByteArray): Info? = runCatching { parseOrThrow(bytes) }.getOrNull()

    private fun parseOrThrow(bytes: ByteArray): Info? {
        if (bytes.size < HEADER_BYTES || MAGIC.indices.any { bytes[it] != MAGIC[it] }) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val keyTable = buffer.getInt(0x08)
        val dataTable = buffer.getInt(0x0C)
        val entries = buffer.getInt(0x10)
        if (entries < 0 || HEADER_BYTES + entries.toLong() * ENTRY_BYTES > bytes.size) return null
        val strings = mutableMapOf<String, String>()
        for (index in 0 until entries) {
            val entry = HEADER_BYTES + index * ENTRY_BYTES
            val keyOffset = buffer.getShort(entry).toInt() and 0xFFFF
            val format = buffer.getShort(entry + 2).toInt() and 0xFFFF
            val length = buffer.getInt(entry + 4)
            val dataOffset = buffer.getInt(entry + 12)
            if (format != FORMAT_UTF8 && format != FORMAT_UTF8_SPECIAL) continue
            val key = cString(bytes, keyTable + keyOffset, bytes.size - (keyTable + keyOffset))
            if (key != KEY_TITLE && key != KEY_TITLE_ID) continue
            strings[key] = cString(bytes, dataTable + dataOffset, length)
        }
        return Info(
            titleId = strings[KEY_TITLE_ID]?.takeIf { it.isNotBlank() },
            title = strings[KEY_TITLE]?.takeIf { it.isNotBlank() }
        )
    }

    private fun cString(bytes: ByteArray, start: Int, maxLength: Int): String {
        require(start in bytes.indices && maxLength >= 0)
        val end = minOf(bytes.size, start + maxLength)
        var stop = start
        while (stop < end && bytes[stop] != 0.toByte()) stop++
        return String(bytes, start, stop - start, Charsets.UTF_8)
    }
}
