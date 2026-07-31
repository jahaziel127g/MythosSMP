package com.mythossmp.stress

import com.mythossmp.MythosSMP
import com.mythossmp.application.profile.ProfileService
import com.mythossmp.infrastructure.config.ConfigManager
import com.mythossmp.infrastructure.database.DatabaseManager
import com.mythossmp.infrastructure.database.SqlProfileRepository
import java.nio.file.Path
import java.util.logging.Logger
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AsyncLogoutStressTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var server: ServerMock
    private lateinit var plugin: MythosSMP
    private lateinit var fake: FakeScheduler
    private lateinit var databaseManager: DatabaseManager
    private lateinit var repository: SqlProfileRepository
    private lateinit var profileService: ProfileService

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()

        plugin = mock()
        whenever(plugin.server).thenReturn(server)
        whenever(plugin.logger).thenReturn(Logger.getGlobal())
        whenever(plugin.name).thenReturn("MythosSMP")
        whenever(plugin.cooldownService).thenReturn(mock())
        whenever(plugin.vowRegistry).thenReturn(mock())

        val configManager = mock<ConfigManager>()
        whenever(configManager.getDatabasePath()).thenReturn(tempDir.resolve("stress.db").toString())
        whenever(configManager.getPoolSize()).thenReturn(4)

        databaseManager = DatabaseManager(plugin, configManager)
        databaseManager.initialize()

        fake = FakeScheduler()
        repository = SqlProfileRepository(databaseManager)
        profileService = ProfileService(plugin, repository, fake)
    }

    @AfterEach
    fun tearDown() {
        databaseManager.shutdown()
        MockBukkit.unmock()
    }

    @Test
    fun `mass logout of 80 players dispatches all saves to the async pool with zero data loss`() {
        val players = (0 until 80).map { server.addPlayer() }

        players.forEach { p ->
            profileService.onPlayerJoin(PlayerJoinEventCompat.join(p))
            profileService.markDirty(p.uniqueId)
        }
        assertEquals(
            80,
            players.count { profileService.getProfile(it.uniqueId) != null },
            "All 80 profiles must be cached after join"
        )

        players.forEach { p ->
            profileService.onPlayerQuit(PlayerJoinEventCompat.quit(p))
        }

        assertEquals(80, fake.asyncCount, "All 80 profile saves must be pushed to the async pool")
        assertEquals(
            0,
            players.count { profileService.getProfile(it.uniqueId) != null },
            "Cache must be fully drained after the mass logout"
        )

        val persisted = databaseManager.getConnection().use { conn ->
            conn.createStatement().executeQuery("SELECT COUNT(*) FROM mythos_players").use { rs ->
                rs.next()
                rs.getInt(1)
            }
        }
        assertEquals(80, persisted, "All 80 profiles must be persisted with zero data loss")

        players.take(5).forEach { p ->
            val reloaded = repository.findByUuid(p.uniqueId)
            assertNotNull(reloaded, "Profile ${p.name} must be fully persisted")
            assertEquals(p.name, reloaded!!.lastKnownName)
        }

        runBlocking { profileService.flushAll() }

        val persistedAfterFlush = databaseManager.getConnection().use { conn ->
            conn.createStatement().executeQuery("SELECT COUNT(*) FROM mythos_players").use { rs ->
                rs.next()
                rs.getInt(1)
            }
        }
        assertEquals(80, persistedAfterFlush, "No partial or duplicate flush may corrupt the table")
    }

    @Test
    fun `quit with clean profile does not dispatch a save`() {
        val player = server.addPlayer()
        profileService.onPlayerJoin(PlayerJoinEventCompat.join(player))

        profileService.onPlayerQuit(PlayerJoinEventCompat.quit(player))

        assertEquals(0, fake.asyncCount, "Clean profiles must not trigger unnecessary writes")
        assertNull(profileService.getProfile(player.uniqueId))
    }

    private object PlayerJoinEventCompat {
        fun join(player: org.bukkit.entity.Player) =
            org.bukkit.event.player.PlayerJoinEvent(player, Component.empty())

        fun quit(player: org.bukkit.entity.Player) =
            org.bukkit.event.player.PlayerQuitEvent(player, Component.empty())
    }
}
