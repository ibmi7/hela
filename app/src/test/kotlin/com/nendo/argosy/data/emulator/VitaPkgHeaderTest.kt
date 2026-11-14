package com.nendo.argosy.data.emulator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class VitaPkgHeaderTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun zipOf(vararg entries: Pair<String, ByteArray>): File {
        val file = temp.newFile("game.zip")
        ZipOutputStream(file.outputStream()).use { out ->
            entries.forEach { (name, bytes) ->
                out.putNextEntry(ZipEntry(name))
                out.write(bytes)
                out.closeEntry()
            }
        }
        return file
    }

    private fun header(contentId: String, magic: ByteArray = byteArrayOf(0x7F, 0x50, 0x4B, 0x47)): ByteArray {
        val bytes = ByteArray(VitaPkgHeader.HEADER_BYTES)
        magic.copyInto(bytes)
        contentId.toByteArray(Charsets.US_ASCII).copyInto(bytes, VitaPkgHeader.CONTENT_ID_OFFSET)
        return bytes
    }

    @Test
    fun `the title id is read from the content id of a package header`() {
        assertEquals("PCSF00243", VitaPkgHeader.titleIdOf(header("EP9000-PCSF00243_00-KZV0000000000000")))
        assertEquals("PCSF00012", VitaPkgHeader.titleIdOf(header("EP9000-PCSF00012_00-0000000000000000")))
    }

    @Test
    fun `bytes past the header do not change the answer`() {
        val longer = header("UP0082-PCSE00261_00-DANGANRONPA00001") + ByteArray(64) { 0x41 }
        assertEquals("PCSE00261", VitaPkgHeader.titleIdOf(longer))
    }

    @Test
    fun `a file without the package magic has no title id`() {
        val zipMagic = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        assertNull(VitaPkgHeader.titleIdOf(header("EP9000-PCSF00243_00-KZV0000000000000", zipMagic)))
    }

    @Test
    fun `a buffer shorter than the header has no title id`() {
        val truncated = header("EP9000-PCSF00243_00-KZV0000000000000").copyOf(VitaPkgHeader.HEADER_BYTES - 1)
        assertNull(VitaPkgHeader.titleIdOf(truncated))
        assertNull(VitaPkgHeader.titleIdOf(ByteArray(0)))
    }

    @Test
    fun `a malformed content id has no title id`() {
        assertNull(VitaPkgHeader.titleIdOf(header("EP9000-PCS00243_00-KZV00000000000000")))
        assertNull(VitaPkgHeader.titleIdOf(header("EP9000PCSF00243_00-KZV00000000000000")))
        assertNull(VitaPkgHeader.titleIdOf(header("")))
    }

    @Test
    fun `a No-Intro style zip yields the title id of its package`() {
        val pkg = header("EP9000-PCSF00243_00-KZV0000000000000") + ByteArray(4096)
        val zip = zipOf("work.bin" to ByteArray(512), "kUrQRHrASnoSVif.PKG" to pkg)
        assertEquals("PCSF00243", ZipFile(zip).use { VitaPkgHeader.titleIdIn(it) })
    }

    @Test
    fun `a zip without a package has no title id`() {
        val zip = zipOf("work.bin" to ByteArray(512), "eboot.bin" to ByteArray(128))
        assertNull(ZipFile(zip).use { VitaPkgHeader.titleIdIn(it) })
    }

    @Test
    fun `a package entry shorter than the header has no title id`() {
        val zip = zipOf("game.pkg" to header("EP9000-PCSF00243_00-KZV0000000000000").copyOf(40))
        assertNull(ZipFile(zip).use { VitaPkgHeader.titleIdIn(it) })
    }

    @Test
    fun `a Vita zip resolves to an exact-folder save id read from the file`() {
        val pkg = header("EP9000-PCSF00243_00-KZV0000000000000") + ByteArray(1024)
        val zip = zipOf("work.bin" to ByteArray(512), "random.pkg" to pkg)
        val expected = TitleIdResult(titleId = "PCSF00243", fromBinary = true)
        assertEquals(expected, VitaPkgHeader.titleIdResultOf(zip, "psvita"))
        assertEquals(expected, VitaPkgHeader.titleIdResultOf(zip, "vita"))
        assertEquals("PCSF00243", expected.saveId)
        assertEquals(TitleIdResult.SaveUsage.FOLDER_EXACT, expected.usage)
    }

    @Test
    fun `other platforms and other files resolve to nothing`() {
        val pkg = header("EP9000-PCSF00243_00-KZV0000000000000") + ByteArray(1024)
        val zip = zipOf("random.pkg" to pkg)
        assertNull(VitaPkgHeader.titleIdResultOf(zip, "psp"))
        assertNull(VitaPkgHeader.titleIdResultOf(temp.newFile("game.vpk"), "psvita"))
        assertNull(VitaPkgHeader.titleIdResultOf(File(temp.root, "missing.zip"), "psvita"))
        val notZip = temp.newFile("broken.zip").apply { writeBytes(ByteArray(64)) }
        assertNull(VitaPkgHeader.titleIdResultOf(notZip, "psvita"))
    }
}
