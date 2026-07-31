package com.mythossmp.task

import com.mythossmp.MythosSMP
import com.mythossmp.application.trial.TrialService
import com.mythossmp.presentation.display.StatusDisplay
import java.time.Duration
import java.util.logging.Logger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class SchedulerAbstractionTest {

    private lateinit var fake: FakeScheduler
    private lateinit var plugin: MythosSMP

    @BeforeEach
    fun setUp() {
        fake = FakeScheduler()
        plugin = mock(MythosSMP::class.java)
        whenever(plugin.logger).thenReturn(Logger.getGlobal())
    }

    @Test
    fun `later tasks are deferred until explicitly run`() {
        var ran = false
        fake.later(Duration.ofMillis(50)) { ran = true }
        assertFalse(ran)
        fake.runAllLater()
        assertTrue(ran)
    }

    @Test
    fun `repeating tasks register and can be cancelled`() {
        var ticks = 0
        val cancellable = fake.repeat(Duration.ofSeconds(1)) { ticks++ }
        fake.runAllRepeating()
        fake.runAllRepeating()
        assertEquals(2, ticks)
        cancellable.cancel()
        fake.runAllRepeating()
        assertEquals(2, ticks)
        assertTrue(fake.cancelledRepeats.contains(cancellable))
    }

    @Test
    fun `sync executes immediately and counts calls`() {
        var ran = false
        fake.sync { ran = true }
        assertTrue(ran)
        assertEquals(1, fake.syncCount)
    }

    @Test
    fun `TrialService registers a 1-second repeating tick and cancels on stop`() {
        val service = TrialService(plugin, fake)
        service.start()
        assertTrue(fake.hasRepeating(Duration.ofSeconds(1)))
        fake.runAllRepeating()
        service.stop()
        assertEquals(1, fake.cancelledRepeats.size)
    }

    @Test
    fun `StatusDisplay registers a 2-second repeating tick and cancels on stop`() {
        val display = StatusDisplay(plugin, fake)
        display.start()
        assertTrue(fake.hasRepeating(Duration.ofSeconds(2)))
        display.stop()
        assertEquals(1, fake.cancelledRepeats.size)
    }
}
