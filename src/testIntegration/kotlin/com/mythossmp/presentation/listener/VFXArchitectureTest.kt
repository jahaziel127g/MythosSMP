package com.mythossmp.presentation.listener

import com.mythossmp.MythosSMP
import com.mythossmp.application.vfx.VFXBroadcastService
import com.mythossmp.domain.event.VowVFXEvent
import org.bukkit.Location
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.plugin.PluginMock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class VFXArchitectureTest {

    private lateinit var server: ServerMock
    private lateinit var plugin: PluginMock
    private lateinit var vfxService: VFXBroadcastService
    private lateinit var capturedEvents: MutableList<VowVFXEvent>

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.createMockPlugin("MythosSMP")

        val mockPlugin = mock<MythosSMP>()
        whenever(mockPlugin.server).thenReturn(server)
        whenever(mockPlugin.name).thenReturn("MythosSMP")

        vfxService = VFXBroadcastService(mockPlugin)
        capturedEvents = mutableListOf()

        server.pluginManager.registerEvents(object : org.bukkit.event.Listener {
            @org.bukkit.event.EventHandler
            fun onVFX(event: VowVFXEvent) {
                capturedEvents.add(event)
            }
        }, plugin)
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `VFX pipeline publishes and dispatches domain events`() {
        val world = server.addSimpleWorld("test")
        val location = Location(world, 10.0, 20.0, 30.0)

        vfxService.publish(VowVFXEvent.LokiBackstab(location))

        assertEquals(1, capturedEvents.size, "VowVFXEvent should be captured by presentation listener")
        val event = capturedEvents[0]
        assertTrue(event is VowVFXEvent.LokiBackstab, "Captured event type should be LokiBackstab")
        assertEquals(10.0, event.location.x, 0.01)
        assertEquals(20.0, event.location.y, 0.01)
        assertEquals(30.0, event.location.z, 0.01)
    }

    @Test
    fun `VFX pipeline handles multiple event types`() {
        val world = server.addSimpleWorld("test")
        val loc = Location(world, 0.0, 0.0, 0.0)

        vfxService.publish(VowVFXEvent.HadesDeathDefianceArm(loc))
        vfxService.publish(VowVFXEvent.LokiVeilActivate(loc))

        assertEquals(2, capturedEvents.size, "Both events should be captured")
        assertTrue(capturedEvents[0] is VowVFXEvent.HadesDeathDefianceArm)
        assertTrue(capturedEvents[1] is VowVFXEvent.LokiVeilActivate)
    }

    @Test
    fun `VowVFXEvent registry resolves under MockBukkit harness`() {
        val particle = org.bukkit.Particle.SCULK_SOUL
        assertNotNull(particle, "SCULK_SOUL particle should resolve under MockBukkit")
    }
}
