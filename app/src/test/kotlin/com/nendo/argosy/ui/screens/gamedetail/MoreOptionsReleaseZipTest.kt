package com.nendo.argosy.ui.screens.gamedetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MoreOptionsReleaseZipTest {

    @Test
    fun `a releasable downloaded game offers release zip just before delete`() {
        val options = buildMoreOptions(
            MoreOptionsContext(isRommGame = true, isDownloaded = true, platformSlug = "psvita", canReleaseZip = true)
        )

        assertEquals(MoreOptionAction.ReleaseZip, options[options.indexOf(MoreOptionAction.Delete) - 1])
    }

    @Test
    fun `release zip is hidden unless the game is releasable and downloaded`() {
        val notReleasable = buildMoreOptions(MoreOptionsContext(isRommGame = true, isDownloaded = true))
        val notDownloaded = buildMoreOptions(MoreOptionsContext(isRommGame = true, canReleaseZip = true))

        assertFalse(notReleasable.contains(MoreOptionAction.ReleaseZip))
        assertFalse(notDownloaded.contains(MoreOptionAction.ReleaseZip))
    }
}
