package com.nendo.argosy.data.emulator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VitaParamSfoTest {

    private fun sfo(vararg entries: Triple<String, Int, ByteArray>): ByteArray {
        val keys = ByteArrayOutputStream()
        val data = ByteArrayOutputStream()
        val index = ByteBuffer.allocate(entries.size * 16).order(ByteOrder.LITTLE_ENDIAN)
        entries.forEach { (key, format, value) ->
            index.putShort(keys.size().toShort())
            index.putShort(format.toShort())
            index.putInt(value.size)
            index.putInt(value.size)
            index.putInt(data.size())
            keys.write(key.toByteArray(Charsets.US_ASCII) + 0)
            data.write(value)
        }
        val keyTable = 0x14 + entries.size * 16
        val dataTable = keyTable + keys.size()
        val header = ByteBuffer.allocate(0x14).order(ByteOrder.LITTLE_ENDIAN)
            .put(byteArrayOf(0x00, 0x50, 0x53, 0x46))
            .putInt(0x0101)
            .putInt(keyTable)
            .putInt(dataTable)
            .putInt(entries.size)
        return header.array() + index.array() + keys.toByteArray() + data.toByteArray()
    }

    private fun utf8(text: String) = Triple(text, 0x0204, text.toByteArray(Charsets.UTF_8) + 0)

    @Test
    fun `title and title id are read from their utf8 entries`() {
        val bytes = sfo(
            Triple("APP_VER", 0x0204, "01.00".toByteArray() + 0),
            Triple("ATTRIBUTE", 0x0404, byteArrayOf(0, 0, 0, 0)),
            Triple("TITLE", 0x0204, "Persona 4 GOLDEN".toByteArray() + 0),
            Triple("TITLE_ID", 0x0204, "PCSB00245".toByteArray() + 0)
        )

        assertEquals(VitaParamSfo.Info("PCSB00245", "Persona 4 GOLDEN"), VitaParamSfo.parse(bytes))
    }

    @Test
    fun `a japanese title keeps its characters`() {
        val bytes = sfo(Triple("TITLE", 0x0204, "キャサリン・フルボディ".toByteArray(Charsets.UTF_8) + 0))

        assertEquals("キャサリン・フルボディ", VitaParamSfo.parse(bytes)?.title)
    }

    @Test
    fun `bytes without the psf magic are not an sfo`() {
        assertNull(VitaParamSfo.parse("not an sfo at all".toByteArray()))
    }

    @Test
    fun `a table pointing outside the buffer is rejected`() {
        val bytes = sfo(utf8("TITLE")).copyOf(0x20)

        assertNull(VitaParamSfo.parse(bytes))
    }
}
