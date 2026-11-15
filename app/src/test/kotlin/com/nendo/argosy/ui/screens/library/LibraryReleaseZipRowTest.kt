package com.nendo.argosy.ui.screens.library

import com.nendo.argosy.data.model.GameSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LibraryReleaseZipRowTest {

    private fun game(isDownloaded: Boolean) = LibraryGameUi(
        id = 1,
        title = "Killzone",
        sortTitle = "killzone",
        platformId = 5,
        platformSlug = "psvita",
        platformDisplayName = "PS Vita",
        coverPath = null,
        source = GameSource.ROMM_SYNCED,
        isFavorite = false,
        isDownloaded = isDownloaded,
        isRommGame = true,
        isAndroidApp = false,
        emulatorName = null,
        listDetails = com.nendo.argosy.ui.common.GameListDetails()
    )

    @Test
    fun `a releasable game offers release zip just before delete`() {
        val rows = libraryQuickMenuRows(game(isDownloaded = true), false, false, canReleaseZip = true)

        assertEquals(LibraryQuickMenuRow.RELEASE_ZIP, rows[rows.indexOf(LibraryQuickMenuRow.DELETE) - 1])
        assertFalse(LibraryQuickMenuRow.RELEASE_ZIP.isDangerous)
    }

    @Test
    fun `release zip is hidden unless the game is releasable and downloaded`() {
        assertFalse(libraryQuickMenuRows(game(isDownloaded = true), false, false).contains(LibraryQuickMenuRow.RELEASE_ZIP))
        assertFalse(
            libraryQuickMenuRows(game(isDownloaded = false), false, false, canReleaseZip = true)
                .contains(LibraryQuickMenuRow.RELEASE_ZIP)
        )
    }
}
