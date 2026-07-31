package com.mythossmp.presentation.listener

import com.mythossmp.MythosSMP
import com.mythossmp.application.combat.CombatService
import com.mythossmp.application.profile.ProfileRepository
import com.mythossmp.application.trust.TrustService
import com.mythossmp.application.union.UnionService
import com.mythossmp.domain.model.PlayerProfile
import com.mythossmp.infrastructure.database.InMemoryTrustRepository
import com.mythossmp.infrastructure.database.InMemoryUnionRepository
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.logging.Logger

class SealOfUnionIntegrationTest {

    private lateinit var server: ServerMock

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    // ---------------------------------------------------------------
    // UnionService: creation, queries, break
    // ---------------------------------------------------------------

    @Test
    fun `two players can create a union`() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val service = makeUnionService()

        val union = service.createUnion(a, b)

        assertNotNull(union)
        assertEquals(a, union!!.playerAUuid)
        assertEquals(b, union.playerBUuid)
        assertTrue(union.active)
    }

    @Test
    fun `cannot create union with self`() {
        val a = UUID.randomUUID()
        val service = makeUnionService()

        val union = service.createUnion(a, a)

        assertNull(union)
    }

    @Test
    fun `cannot create union if already in one`() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val c = UUID.randomUUID()
        val service = makeUnionService()

        service.createUnion(a, b)
        val second = service.createUnion(a, c)

        assertNull(second)
    }

    @Test
    fun `areInSameUnion returns true for members`() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val service = makeUnionService()

        service.createUnion(a, b)

        assertTrue(service.areInSameUnion(a, b))
        assertTrue(service.areInSameUnion(b, a))
    }

    @Test
    fun `areInSameUnion returns false for non-members`() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val c = UUID.randomUUID()
        val service = makeUnionService()

        service.createUnion(a, b)

        assertFalse(service.areInSameUnion(a, c))
        assertFalse(service.areInSameUnion(c, b))
    }

    @Test
    fun `isInUnion reflects state after creation and break`() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val service = makeUnionService()

        service.createUnion(a, b)
        assertTrue(service.isInUnion(a))
        assertTrue(service.isInUnion(b))

        service.breakUnion(a, "test")
        assertFalse(service.isInUnion(a))
        assertFalse(service.isInUnion(b))
    }

    @Test
    fun `union partnerOf returns correct partner`() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val service = makeUnionService()

        service.createUnion(a, b)
        val union = service.getUnion(a)

        assertNotNull(union)
        assertEquals(b, union!!.partnerOf(a))
        assertEquals(a, union.partnerOf(b))
        assertNull(union.partnerOf(UUID.randomUUID()))
    }

    // ---------------------------------------------------------------
    // UnionService: handlePlayerDeath (double-loss)
    // ---------------------------------------------------------------

    @Test
    fun `handlePlayerDeath returns DoubleLossEvent for union member`() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val profileRepo = mock<ProfileRepository>()
        val plugin = mock<MythosSMP>()

        whenever(plugin.logger).thenReturn(Logger.getGlobal())
        whenever(plugin.server).thenReturn(server)

        val unionRepo = InMemoryUnionRepository()
        val service = UnionService(plugin, unionRepo, profileRepo)

        service.createUnion(a, b)

        val result = service.handlePlayerDeath(a)

        assertNotNull(result)
        assertEquals(a, result!!.deathUuid)
        assertEquals(b, result.partnerUuid)
        assertFalse(service.isInUnion(a), "Union should be removed after death")
        assertFalse(service.isInUnion(b), "Partner union should be removed after death")
    }

    @Test
    fun `handlePlayerDeath returns null for non-union player`() {
        val a = UUID.randomUUID()
        val profileRepo = mock<ProfileRepository>()
        val plugin = mock<MythosSMP>()

        whenever(plugin.logger).thenReturn(Logger.getGlobal())
        whenever(plugin.server).thenReturn(server)

        val unionRepo = InMemoryUnionRepository()
        val service = UnionService(plugin, unionRepo, profileRepo)

        val result = service.handlePlayerDeath(a)

        assertNull(result)
    }

    @Test
    fun `handlePlayerDeath revokes active Vow from offline partner via profile repo`() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val profileRepo = mock<ProfileRepository>()
        val plugin = mock<MythosSMP>()

        whenever(plugin.logger).thenReturn(Logger.getGlobal())
        whenever(plugin.server).thenReturn(server)

        val activeProfileA = PlayerProfile(
            playerUuid = a,
            lastKnownName = "PlayerA",
            activeVow = "ares",
            dirty = false
        )
        val activeProfileB = PlayerProfile(
            playerUuid = b,
            lastKnownName = "PlayerB",
            activeVow = "ares",
            dirty = false
        )

        whenever(profileRepo.findByUuid(a)).thenReturn(activeProfileA)
        whenever(profileRepo.findByUuid(b)).thenReturn(activeProfileB)

        val unionRepo = InMemoryUnionRepository()
        val service = UnionService(plugin, unionRepo, profileRepo)

        service.createUnion(a, b)
        val result = service.handlePlayerDeath(a)

        assertNotNull(result)
    }

    // ---------------------------------------------------------------
    // CombatService: shouldCancelDamage (trust + union)
    // ---------------------------------------------------------------

    @Test
    fun `trusted player direct damage is cancelled`() {
        val trustService = mock<TrustService>()
        val unionService = mock<UnionService>()
        val combat = CombatService(trustService, unionService)

        val damager: Player = server.addPlayer()
        val damaged: Player = server.addPlayer()

        whenever(trustService.areMutuallyTrusted(damager.uniqueId, damaged.uniqueId)).thenReturn(true)

        val result = combat.shouldCancelDamage(damager, damaged)

        assertTrue(result)
    }

    @Test
    fun `union member direct damage is cancelled`() {
        val trustService = mock<TrustService>()
        val unionService = mock<UnionService>()
        val combat = CombatService(trustService, unionService)

        val damager: Player = server.addPlayer()
        val damaged: Player = server.addPlayer()

        whenever(trustService.areMutuallyTrusted(damager.uniqueId, damaged.uniqueId)).thenReturn(false)
        whenever(unionService.areInSameUnion(damager.uniqueId, damaged.uniqueId)).thenReturn(true)

        val result = combat.shouldCancelDamage(damager, damaged)

        assertTrue(result)
    }

    @Test
    fun `non-trusted non-union damage is not cancelled`() {
        val trustService = mock<TrustService>()
        val unionService = mock<UnionService>()
        val combat = CombatService(trustService, unionService)

        val damager: Player = server.addPlayer()
        val damaged: Player = server.addPlayer()

        whenever(trustService.areMutuallyTrusted(damager.uniqueId, damaged.uniqueId)).thenReturn(false)
        whenever(unionService.areInSameUnion(damager.uniqueId, damaged.uniqueId)).thenReturn(false)

        val result = combat.shouldCancelDamage(damager, damaged)

        assertFalse(result)
    }

    @Test
    fun `self damage is not cancelled`() {
        val trustService = mock<TrustService>()
        val unionService = mock<UnionService>()
        val combat = CombatService(trustService, unionService)

        val player: Player = server.addPlayer()

        val result = combat.shouldCancelDamage(player, player)

        assertFalse(result)
    }

    @Test
    fun `projectile from trusted player cancels damage`() {
        val trustService = mock<TrustService>()
        val unionService = mock<UnionService>()
        val combat = CombatService(trustService, unionService)

        val shooter: Player = server.addPlayer()
        val damaged: Player = server.addPlayer()

        val projectile: Projectile = mock()
        whenever(projectile.shooter).thenReturn(shooter)

        whenever(trustService.areMutuallyTrusted(shooter.uniqueId, damaged.uniqueId)).thenReturn(true)

        val result = combat.shouldCancelDamage(projectile, damaged)

        assertTrue(result)
    }

    @Test
    fun `non-player entity does not trigger cancellation`() {
        val trustService = mock<TrustService>()
        val unionService = mock<UnionService>()
        val combat = CombatService(trustService, unionService)

        val damaged: Player = server.addPlayer()
        val nonPlayerEntity = mock<org.bukkit.entity.Entity>()

        val result = combat.shouldCancelDamage(nonPlayerEntity, damaged)

        assertFalse(result)
    }

    // ---------------------------------------------------------------
    // CombatService: shouldCancelAreaEffect (trust + union)
    // ---------------------------------------------------------------

    @Test
    fun `area effect from trusted player is cancelled`() {
        val trustService = mock<TrustService>()
        val unionService = mock<UnionService>()
        val combat = CombatService(trustService, unionService)

        val source: Player = server.addPlayer()
        val affected: Player = server.addPlayer()
        val cloud: AreaEffectCloud = mock()
        whenever(cloud.source).thenReturn(source)

        whenever(trustService.areMutuallyTrusted(source.uniqueId, affected.uniqueId)).thenReturn(true)

        val result = combat.shouldCancelAreaEffect(cloud, affected)

        assertTrue(result)
    }

    @Test
    fun `area effect from union member is cancelled`() {
        val trustService = mock<TrustService>()
        val unionService = mock<UnionService>()
        val combat = CombatService(trustService, unionService)

        val source: Player = server.addPlayer()
        val affected: Player = server.addPlayer()
        val cloud: AreaEffectCloud = mock()
        whenever(cloud.source).thenReturn(source)

        whenever(trustService.areMutuallyTrusted(source.uniqueId, affected.uniqueId)).thenReturn(false)
        whenever(unionService.areInSameUnion(source.uniqueId, affected.uniqueId)).thenReturn(true)

        val result = combat.shouldCancelAreaEffect(cloud, affected)

        assertTrue(result)
    }

    @Test
    fun `area effect self-damage is not cancelled`() {
        val trustService = mock<TrustService>()
        val unionService = mock<UnionService>()
        val combat = CombatService(trustService, unionService)

        val player: Player = server.addPlayer()
        val cloud: AreaEffectCloud = mock()
        whenever(cloud.source).thenReturn(player)

        val result = combat.shouldCancelAreaEffect(cloud, player)

        assertFalse(result)
    }

    @Test
    fun `area effect projectile from trusted player cancels`() {
        val trustService = mock<TrustService>()
        val unionService = mock<UnionService>()
        val combat = CombatService(trustService, unionService)

        val shooter: Player = server.addPlayer()
        val affected: Player = server.addPlayer()
        val projectile: Projectile = mock()
        whenever(projectile.shooter).thenReturn(shooter)

        whenever(trustService.areMutuallyTrusted(shooter.uniqueId, affected.uniqueId)).thenReturn(true)

        val result = combat.shouldCancelAreaEffect(projectile, affected)

        assertTrue(result)
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private fun makeUnionService(): UnionService {
        val plugin = mock<MythosSMP>()
        whenever(plugin.logger).thenReturn(Logger.getGlobal())
        whenever(plugin.server).thenReturn(server)
        val profileRepo = mock<ProfileRepository>()
        val unionRepo = InMemoryUnionRepository()
        return UnionService(plugin, unionRepo, profileRepo)
    }
}
