package com.nendo.argosy.data.emulator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class Vita3kStubTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `the stub keeps the zip name so the save channel name does not change`() {
        val zip = File(temp.root, "Killzone - Mercenary (Europe).zip")
        val stub = Vita3kStub.stubFor(zip)
        assertEquals("Killzone - Mercenary (Europe).vita3k", stub.name)
        assertEquals(zip.nameWithoutExtension, stub.nameWithoutExtension)
        assertTrue(Vita3kStub.isStub(stub))
        assertFalse(Vita3kStub.isStub(zip))
    }

    @Test
    fun `a written title id reads back and anything else reads as nothing`() {
        val stub = File(temp.root, "game.vita3k")
        Vita3kStub.write(stub, "PCSF00243")
        assertEquals("PCSF00243", Vita3kStub.read(stub))

        stub.writeText("not an id")
        assertNull(Vita3kStub.read(stub))
        assertNull(Vita3kStub.read(File(temp.root, "missing.vita3k")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a malformed title id is refused`() {
        Vita3kStub.write(File(temp.root, "game.vita3k"), "PCSF243")
    }

    @Test
    fun `the package header reader understands a stub`() {
        val stub = File(temp.root, "game.vita3k").apply { writeText("PCSB00866\n") }
        assertEquals("PCSB00866", VitaPkgHeader.titleIdResultOf(stub, "psvita")?.titleId)
        assertNull(VitaPkgHeader.titleIdResultOf(stub, "psp"))
    }
}
