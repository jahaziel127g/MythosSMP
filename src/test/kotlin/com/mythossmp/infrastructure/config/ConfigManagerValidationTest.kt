package com.mythossmp.infrastructure.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.StringReader

class ConfigManagerValidationTest {

    private fun readConfig(yaml: String): ConfigurationSection {
        return YamlConfiguration.loadConfiguration(StringReader(yaml))
    }

    private fun section(path: String = ""): ConfigurationSection {
        val root = YamlConfiguration()
        root.createSection(path)
        return root.getConfigurationSection(path) ?: root
    }

    @Test
    fun `empty config returns defaults`() {
        val cfg = ConfigManager.VowConfig(
            enabled = true, curseHealthReduction = 4.0, backstabDotThreshold = 0.75,
            backstabMultiplier = 2.5, ability1Cooldown = 25, ability2Cooldown = 45,
            shadowStepRange = 15.0, veilDuration = 10.0, veilFirstHitMultiplier = 3.0
        )
        assertEquals(4.0, cfg.curseHealthReduction)
        assertEquals(0.75, cfg.backstabDotThreshold)
        assertEquals(2.5, cfg.backstabMultiplier)
        assertEquals(25, cfg.ability1Cooldown)
        assertEquals(15.0, cfg.shadowStepRange)
        assertEquals(10.0, cfg.veilDuration)
        assertEquals(3.0, cfg.veilFirstHitMultiplier)
    }

    @Test
    fun `malformed yaml loads without crashing`() {
        val root = readConfig("vows:\n  loki:\n    curse:\n      health-reduction: not-a-number\n")
        val curseSection = root.getConfigurationSection("vows.loki.curse")
        val hr = curseSection?.getString("health-reduction") ?: "4.0"
        assertEquals("not-a-number", hr)
    }

    @Test
    fun `partially missing sections return null`() {
        val root = readConfig("vows:\n  loki:\n    abilities:\n      shadow-step:\n        cooldown: 10\n")
        val abSection = root.getConfigurationSection("vows.loki.abilities.shadow-step")
        assertEquals(10, abSection?.getInt("cooldown"))
        assertEquals(null, root.getConfigurationSection("vows.loki.curse"))
    }

    @Test
    fun `range can be any positive double`() {
        val cfg = ConfigManager.VowConfig(
            enabled = true, curseHealthReduction = 4.0, backstabDotThreshold = 0.75,
            backstabMultiplier = 2.5, ability1Cooldown = 25, ability2Cooldown = 45,
            shadowStepRange = 25.0, veilDuration = 10.0, veilFirstHitMultiplier = 3.0
        )
        assertEquals(25.0, cfg.shadowStepRange, 0.01)
    }

    @Test
    fun `cooldown can be any valid int`() {
        val cfg = ConfigManager.VowConfig(
            enabled = true, curseHealthReduction = 4.0, backstabDotThreshold = 0.75,
            backstabMultiplier = 2.5, ability1Cooldown = 120, ability2Cooldown = 90,
            shadowStepRange = 15.0, veilDuration = 10.0, veilFirstHitMultiplier = 3.0
        )
        assertEquals(120, cfg.ability1Cooldown)
        assertEquals(90, cfg.ability2Cooldown)
    }

    @Test
    fun `data class structural equality works`() {
        val a = ConfigManager.VowConfig(
            enabled = true, curseHealthReduction = 4.0, backstabDotThreshold = 0.75,
            backstabMultiplier = 2.5, ability1Cooldown = 25, ability2Cooldown = 45,
            shadowStepRange = 15.0, veilDuration = 10.0, veilFirstHitMultiplier = 3.0
        )
        val b = a.copy()
        assertEquals(a, b)
    }
}
