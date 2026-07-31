package com.mythossmp.domain.vow

import com.mythossmp.MythosSMP
import com.mythossmp.application.profile.ProfileService
import com.mythossmp.domain.model.PlayerProfile
import com.mythossmp.infrastructure.config.ConfigManager
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffectType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class HadesIntegrationTest {

    private lateinit var server: ServerMock

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `registry resolves soul particle type`() {
        val particle = org.bukkit.Particle.SCULK_SOUL
        assertNotNull(particle, "SculkSoul particle should load under MockBukkit harness")
    }

    @Test
    fun `registry resolves potion effect types for death defiance`() {
        assertNotNull(PotionEffectType.SPEED, "Speed effect should resolve")
        assertNotNull(PotionEffectType.RESISTANCE, "Resistance effect should resolve")
    }

    @Test
    fun `material registry resolves for soul burst`() {
        val material = Material.STONE
        assertTrue(material.isBlock, "Material registry should be populated")
    }

    @Test
    fun `death defiance save sets health to one`() {
        val plugin = mock<MythosSMP>()
        whenever(plugin.logger).thenReturn(java.util.logging.Logger.getGlobal())
        whenever(plugin.server).thenReturn(server)
        whenever(plugin.name).thenReturn("MythosSMP")

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

        val profileService = mock<ProfileService>()
        whenever(plugin.profileService).thenReturn(profileService)

        val cm = ConfigManager(plugin)
        cm.load()
        whenever(plugin.configManager).thenReturn(cm)

        val player: Player = server.addPlayer()
        val playerProfile = PlayerProfile(
            playerUuid = player.uniqueId,
            lastKnownName = "TestPlayer",
            activeVow = "hades",
            dirty = false
        )
        whenever(profileService.getProfile(player.uniqueId)).thenReturn(playerProfile)

        player.health = 20.0
        player.setLastDamage(0.0)

        VowHades.passive.apply(player)

        val javaField = VowHades::class.java.classLoader.loadClass(
            "com.mythossmp.domain.vow.VowHadesKt"
        ).getDeclaredField("soulChargesMap")
        javaField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val chargesMap = javaField.get(null) as java.util.concurrent.ConcurrentHashMap<java.util.UUID, Int>
        val cfg = cm.getVowConfig("hades")
        chargesMap[player.uniqueId] = cfg.maxCharges

        val dd = DeathDefiance()
        val armed = dd.execute(player, plugin)
        assertTrue(armed, "Death Defiance should arm when at max charges")

        val damageEvent = EntityDamageEvent(player, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 100.0)

        val listener = HadesListener()
        listener.onDeathDefianceSave(damageEvent)

        assertTrue(damageEvent.isCancelled, "Death Defiance should cancel fatal damage")
        assertEquals(1.0, player.health, 0.01, "Player should survive with 1 HP")
        assertFalse(chargesMap.containsKey(player.uniqueId), "All soul charges should be consumed")
    }

    @Test
    fun `soul charges can be stored per player`() {
        val plugin = mock<MythosSMP>()
        whenever(plugin.logger).thenReturn(java.util.logging.Logger.getGlobal())
        whenever(plugin.server).thenReturn(server)
        whenever(plugin.name).thenReturn("MythosSMP")

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

        val profileService = mock<ProfileService>()
        whenever(plugin.profileService).thenReturn(profileService)

        val cm = ConfigManager(plugin)
        cm.load()
        whenever(plugin.configManager).thenReturn(cm)

        val player: Player = server.addPlayer()
        val playerProfile = PlayerProfile(
            playerUuid = player.uniqueId,
            lastKnownName = "TestPlayer",
            activeVow = "hades",
            dirty = false
        )
        whenever(profileService.getProfile(player.uniqueId)).thenReturn(playerProfile)

        VowHades.passive.apply(player)

        val javaField = VowHades::class.java.classLoader.loadClass(
            "com.mythossmp.domain.vow.VowHadesKt"
        ).getDeclaredField("soulChargesMap")
        javaField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val chargesMap = javaField.get(null) as java.util.concurrent.ConcurrentHashMap<java.util.UUID, Int>

        assertEquals(0, chargesMap[player.uniqueId], "Initial charges should be 0")

        chargesMap.compute(player.uniqueId) { _, current ->
            val cur = current ?: 0
            if (cur >= 5) cur else cur + 1
        }
        assertEquals(1, chargesMap[player.uniqueId], "After one kill, charges should be 1")

        chargesMap.compute(player.uniqueId) { _, current ->
            val cur = current ?: 0
            if (cur >= 5) cur else cur + 1
        }
        assertEquals(2, chargesMap[player.uniqueId], "After two kills, charges should be 2")
    }
}
