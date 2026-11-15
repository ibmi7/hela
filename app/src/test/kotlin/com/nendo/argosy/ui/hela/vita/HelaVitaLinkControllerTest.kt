package com.nendo.argosy.ui.hela.vita

import com.nendo.argosy.core.notification.NotificationManager
import com.nendo.argosy.data.emulator.Vita3kLink
import com.nendo.argosy.data.emulator.Vita3kLinkMatcher.Match
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HelaVitaLinkControllerTest {

    private val link: Vita3kLink = mockk()
    private val controller = HelaVitaLinkController(link, mockk<NotificationManager>(relaxed = true))

    private val fifa = Match(238, "FIFA 15", "PCSB00604", "FIFA 15")
    private val persona = Match(226, "Persona 4 Golden", "PCSB00245", "Persona 4 GOLDEN")
    private val tennis = Match(237, "Virtua Tennis 4: World Tour Edition", "PCSB00031", "Virtua Tennis 4")

    @Test
    fun `sure matches link at once and title matches wait unchecked in the list`() = runTest {
        coEvery { link.scan(5) } returns Vita3kLink.Scan.Found(listOf(fifa), listOf(persona, tennis))
        coEvery { link.link(any(), any()) } returns true

        controller.start(this, 5)
        advanceUntilIdle()

        coVerify(exactly = 1) { link.link(238, "PCSB00604") }
        coVerify(exactly = 0) { link.link(226, any()) }
        val state = controller.state.value
        assertTrue(state.visible)
        assertEquals(listOf(226L, 237L), state.rows.map { it.gameId })
        assertTrue(state.checked.isEmpty())
    }

    @Test
    fun `confirm links only the checked rows`() = runTest {
        coEvery { link.scan(5) } returns Vita3kLink.Scan.Found(emptyList(), listOf(persona, tennis))
        coEvery { link.link(any(), any()) } returns true
        controller.start(this, 5)
        advanceUntilIdle()

        controller.activateFocused(this)
        controller.moveFocus(5)
        controller.activateFocused(this)
        advanceUntilIdle()

        coVerify(exactly = 1) { link.link(226, "PCSB00245") }
        coVerify(exactly = 0) { link.link(237, any()) }
        assertFalse(controller.state.value.visible)
    }

    @Test
    fun `left and right check a row and move between the buttons`() = runTest {
        coEvery { link.scan(5) } returns Vita3kLink.Scan.Found(emptyList(), listOf(persona))
        controller.start(this, 5)
        advanceUntilIdle()

        controller.moveHorizontal(1)
        assertEquals(setOf(226L), controller.state.value.checked)
        controller.moveHorizontal(-1)
        assertTrue(controller.state.value.checked.isEmpty())

        controller.moveFocus(1)
        assertEquals(controller.state.value.cancelIndex, controller.state.value.focusIndex)
        controller.moveHorizontal(1)
        assertEquals(controller.state.value.confirmIndex, controller.state.value.focusIndex)
    }

    @Test
    fun `cancel links nothing`() = runTest {
        coEvery { link.scan(5) } returns Vita3kLink.Scan.Found(emptyList(), listOf(persona))
        controller.start(this, 5)
        advanceUntilIdle()

        controller.setChecked(226, true)
        controller.dismiss()
        advanceUntilIdle()

        coVerify(exactly = 0) { link.link(any(), any()) }
        assertFalse(controller.state.value.visible)
    }
}
