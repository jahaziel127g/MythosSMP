package com.mythossmp.domain.vow

import com.mythossmp.MythosSMP
import com.mythossmp.infrastructure.config.ConfigManager
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.logging.Logger

class VowHadesTest {

    private lateinit var plugin: MythosSMP
    private lateinit var world: World
    private lateinit var player: Player
    private val playerUuid = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        plugin = mock()
        world = mock()

        val yaml = """
            vows:
              hades:
                enabled: true
                curse:
                  health-reduction: 4.0
                passive:
                  max-charges: 5
                  damage-bonus-per-charge: 0.1
                abilities:
                  death-defiance:
                    cooldown: 90
                  soul-burst:
                    cooldown: 30
                    range: 8.0
                    base-damage: 4.0
        """.trimIndent()
        val config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
            java.io.StringReader(yaml)
        )

        whenever(plugin.config).thenReturn(config)
        whenever(plugin.logger).thenReturn(Logger.getGlobal())
        whenever(plugin.name).thenReturn("MythosSMP")
        whenever(world.name).thenReturn("world")

        player = mock()
        whenever(player.world).thenReturn(world)
        whenever(player.uniqueId).thenReturn(playerUuid)
        whenever(player.name).thenReturn("TestPlayer")
        whenever(player.location).thenReturn(Location(world, 0.0, 0.0, 0.0))
    }

    @Test
    fun `soul harvest damage multiplier with zero charges`() {
        val cm = spyConfigManager()
        whenever(plugin.configManager).thenReturn(cm)
        val cfg = cm.getVowConfig("hades")

        val charges = 0
        val multiplier = 1.0 + (charges * cfg.damageBonusPerCharge)
        assertEquals(1.0, multiplier, 0.001)
    }

    @Test
    fun `soul harvest damage multiplier with one charge`() {
        val cm = spyConfigManager()
        whenever(plugin.configManager).thenReturn(cm)
        val cfg = cm.getVowConfig("hades")

        val charges = 1
        val multiplier = 1.0 + (charges * cfg.damageBonusPerCharge)
        assertEquals(1.1, multiplier, 0.001)
    }

    @Test
    fun `soul harvest damage multiplier at max charges`() {
        val cm = spyConfigManager()
        whenever(plugin.configManager).thenReturn(cm)
        val cfg = cm.getVowConfig("hades")

        val charges = 5
        val multiplier = 1.0 + (charges * cfg.damageBonusPerCharge)
        assertEquals(1.5, multiplier, 0.001)
    }

    @Test
    fun `soul harvest damage multiplier respects configured values`() {
        val cm = spyConfigManager()
        whenever(plugin.configManager).thenReturn(cm)
        val cfg = cm.getVowConfig("hades")

        val charges = 3
        val multiplier = 1.0 + (charges * cfg.damageBonusPerCharge)
        assertEquals(1.3, multiplier, 0.001)
    }

    @Test
    fun `soul burst damage calculation`() {
        val cm = spyConfigManager()
        whenever(plugin.configManager).thenReturn(cm)
        val cfg = cm.getVowConfig("hades")

        val charges = 5
        val expectedDmg = cfg.soulBurstBaseDamage * charges
        assertEquals(20.0, expectedDmg, 0.001)
    }

    @Test
    fun `soul burst with one charge`() {
        val cm = spyConfigManager()
        whenever(plugin.configManager).thenReturn(cm)
        val cfg = cm.getVowConfig("hades")

        val charges = 1
        val expectedDmg = cfg.soulBurstBaseDamage * charges
        assertEquals(4.0, expectedDmg, 0.001)
    }

    @Test
    fun `soul burst with zero charges`() {
        val cm = spyConfigManager()
        whenever(plugin.configManager).thenReturn(cm)
        val cfg = cm.getVowConfig("hades")

        val charges = 0
        val expectedDmg = cfg.soulBurstBaseDamage * charges
        assertEquals(0.0, expectedDmg, 0.001)
    }

    @Test
    fun `charge count respects max charges from config`() {
        val cm = spyConfigManager()
        whenever(plugin.configManager).thenReturn(cm)
        val cfg = cm.getVowConfig("hades")

        val currentCharges = 3
        val result = if (currentCharges >= cfg.maxCharges) currentCharges else currentCharges + 1
        assertEquals(4, result)
    }

    @Test
    fun `charge count capped at max charges`() {
        val cm = spyConfigManager()
        whenever(plugin.configManager).thenReturn(cm)
        val cfg = cm.getVowConfig("hades")

        val currentCharges = 5
        val result = if (currentCharges >= cfg.maxCharges) currentCharges else currentCharges + 1
        assertEquals(5, result)
    }

    private fun spyConfigManager(): ConfigManager {
        org.mockito.kotlin.doNothing().`when`(plugin).saveDefaultConfig()
        org.mockito.kotlin.doNothing().`when`(plugin).reloadConfig()
        val cm = ConfigManager(plugin)
        cm.load()
        return cm
    }
}
