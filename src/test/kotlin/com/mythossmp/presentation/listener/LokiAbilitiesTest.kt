package com.mythossmp.presentation.listener

import com.mythossmp.MythosSMP
import com.mythossmp.domain.vow.ShadowStep
import com.mythossmp.infrastructure.config.ConfigManager
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.logging.Logger

class LokiAbilitiesTest {

    private lateinit var plugin: MythosSMP
    private lateinit var world: World
    private lateinit var player: Player

    @BeforeEach
    fun setUp() {
        plugin = mock()
        world = mock()
        player = mock()

        val yaml = """
            vows:
              loki:
                enabled: true
                curse:
                  health-reduction: 4.0
                passive:
                  backstab-dot-threshold: 0.75
                  backstab-multiplier: 2.5
                abilities:
                  shadow-step:
                    cooldown: 25
                    range: 15.0
                  veil-of-lies:
                    cooldown: 45
                    duration: 10.0
                    first-hit-multiplier: 3.0
        """.trimIndent()
        val config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
            java.io.StringReader(yaml)
        )

        whenever(plugin.config).thenReturn(config)
        whenever(plugin.logger).thenReturn(Logger.getGlobal())
        whenever(plugin.name).thenReturn("MythosSMP")
        whenever(plugin.vfxBroadcastService).thenReturn(mock())
        whenever(world.name).thenReturn("world")
        whenever(player.world).thenReturn(world)
        whenever(player.uniqueId).thenReturn(UUID.randomUUID())
        whenever(player.name).thenReturn("TestPlayer")
        whenever(player.location).thenReturn(Location(world, 0.0, 0.0, 0.0))
    }

    @Test
    fun `shadow step with target teleports player`() {
        val cm = spyConfigManager()
        whenever(plugin.configManager).thenReturn(cm)

        val target: LivingEntity = mock()
        whenever(target.location).thenReturn(Location(world, 10.0, 0.0, 0.0))
        whenever(target.isValid).thenReturn(true)
        whenever(target.isDead).thenReturn(false)
        whenever(world.getHighestBlockYAt(any<Int>(), any<Int>())).thenReturn(0)
        whenever(world.getNearbyEntities(any(), any(), any(), any())).thenReturn(listOf(target))

        val ability = ShadowStep()
        val result = ability.execute(player, plugin)

        assertTrue(result, "Should succeed with a valid target")
        verify(player).teleport(any<Location>())
    }

    @Test
    fun `shadow step no target returns false`() {
        val cm = spyConfigManager()
        whenever(plugin.configManager).thenReturn(cm)
        whenever(world.getNearbyEntities(any(), any(), any(), any())).thenReturn(emptyList())

        val ability = ShadowStep()
        val result = ability.execute(player, plugin)

        assertFalse(result, "Should fail with no targets")
    }

    @Test
    fun `shadow step skips dead entities`() {
        val cm = spyConfigManager()
        whenever(plugin.configManager).thenReturn(cm)
        val deadTarget: LivingEntity = mock()
        whenever(deadTarget.location).thenReturn(Location(world, 5.0, 0.0, 0.0))
        whenever(deadTarget.isValid).thenReturn(false)
        whenever(deadTarget.isDead).thenReturn(true)
        whenever(world.getNearbyEntities(any(), any(), any(), any())).thenReturn(listOf(deadTarget))

        val ability = ShadowStep()
        val result = ability.execute(player, plugin)

        assertFalse(result, "Should fail when only dead entities exist")
    }

    private fun spyConfigManager(): ConfigManager {
        org.mockito.kotlin.doNothing().`when`(plugin).saveDefaultConfig()
        org.mockito.kotlin.doNothing().`when`(plugin).reloadConfig()
        val cm = ConfigManager(plugin)
        cm.load()
        return cm
    }
}
