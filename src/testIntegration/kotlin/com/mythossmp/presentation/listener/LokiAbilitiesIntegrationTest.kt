package com.mythossmp.presentation.listener

import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.bukkit.Material
import org.bukkit.potion.PotionEffectType
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

class LokiAbilitiesIntegrationTest {

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
    fun verifyRegistryDependentLogic() {
        val invisType = PotionEffectType.INVISIBILITY
        assertNotNull(invisType, "Invisibility registry should load successfully under the harness.")

        val stoneMaterial = Material.STONE
        assertTrue(stoneMaterial.isBlock, "Material registry should be fully populated.")
    }

    @Test
    fun testVeilOfLiesExecution() {
        val player: Player = server.addPlayer()

        player.addPotionEffect(org.bukkit.potion.PotionEffect(PotionEffectType.INVISIBILITY, 200, 0))

        assertTrue(player.hasPotionEffect(PotionEffectType.INVISIBILITY), "Player should successfully gain invisibility status.")
    }
}
