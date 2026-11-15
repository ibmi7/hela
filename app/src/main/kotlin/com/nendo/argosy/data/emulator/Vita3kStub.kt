package com.nendo.argosy.data.emulator

import java.io.File

object Vita3kStub {
    const val EXTENSION = "vita3k"
    private val TITLE_ID = Regex("""^[A-Z]{4}\d{5}$""")

    fun isStub(file: File): Boolean = file.extension.equals(EXTENSION, ignoreCase = true)

    fun stubFor(zip: File): File = File(zip.parentFile, "${zip.nameWithoutExtension}.$EXTENSION")

    fun write(stub: File, titleId: String) {
        require(TITLE_ID.matches(titleId)) { "not a Vita title id: $titleId" }
        stub.writeText(titleId)
    }

    fun read(stub: File): String? =
        runCatching { stub.readText().trim() }.getOrNull()?.takeIf { TITLE_ID.matches(it) }
}
