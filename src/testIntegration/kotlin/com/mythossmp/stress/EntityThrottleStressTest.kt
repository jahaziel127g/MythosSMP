package com.mythossmp.stress

import com.mythossmp.MythosSMP
import com.mythossmp.application.trial.TrialService
import com.mythossmp.domain.trial.MobSpawnEntry
import com.mythossmp.domain.trial.TrialConfig
import com.mythossmp.domain.trial.TrialSession
import com.mythossmp.domain.trial.WaveConfig
import java.util.logging.Logger
import org.bukkit.entity.EntityType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class EntityThrottleStressTest {

    private lateinit var server: ServerMock
    private lateinit var plugin: MythosSMP
    private lateinit var fake: FakeScheduler
    private lateinit var trialService: TrialService

    private val spawnMobMethod by lazy {
        TrialService::class.java.getDeclaredMethod(
            "spawnMob",
            TrialSession::class.java,
            EntityType::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        ).apply { isAccessible = true }
    }

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()

        plugin = mock()
        whenever(plugin.server).thenReturn(server)
        whenever(plugin.logger).thenReturn(Logger.getGlobal())
        whenever(plugin.name).thenReturn("MythosSMP")

        fake = FakeScheduler()
        trialService = TrialService(plugin, fake)
        trialService.registerDefaultConfig(TrialConfig(
            key = "stress",
            displayName = "Stress Trial",
            vowKey = "stress",
            description = "Throttle stress trial",
            waves = listOf(WaveConfig(1, listOf(MobSpawnEntry(EntityType.ZOMBIE, 2)))),
            baseHealth = 20.0,
            baseDamage = 2.0,
            timeLimitSeconds = 600,
            maxPartySize = 4
        ))
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `spawning flood inside the trial arena is clipped at 25 custom entities`() {
        val player = server.addPlayer()
        val session = trialService.startTrial(player, "stress")
        assertNotNull(session, "Trial session must start")
        val world = player.world

        repeat(200) {
            spawnMobMethod.invoke(trialService, session, EntityType.ZOMBIE, 1, 1)
        }

        val customCount = world.getEntities().count { trialService.isTrialMob(it.uniqueId) }
        assertEquals(
            25,
            customCount,
            "Active custom entity count must cap at exactly 25 and never spill over"
        )
        assertEquals(25, session!!.spawnedMobs.size, "Session tracking must match the world cap")
    }

    @Test
    fun `cap survives repeated flooding waves`() {
        val player = server.addPlayer()
        val session = trialService.startTrial(player, "stress")
        assertNotNull(session)
        val world = player.world

        repeat(3) {
            repeat(100) { spawnMobMethod.invoke(trialService, session, EntityType.ZOMBIE, 1, 1) }
        }

        val customCount = world.getEntities().count { trialService.isTrialMob(it.uniqueId) }
        assertTrue(customCount <= 25, "Custom entity count must never exceed 25")
        assertEquals(25, customCount)
    }
}
