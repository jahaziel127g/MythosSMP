package com.mythossmp.application.trial

import com.mythossmp.MythosSMP
import com.mythossmp.application.task.Cancellable
import com.mythossmp.application.task.Scheduler
import com.mythossmp.domain.event.TrialCompletedEvent
import com.mythossmp.domain.trial.GeoBoundary
import com.mythossmp.domain.trial.MobScalingFormula
import com.mythossmp.domain.trial.TrialConfig
import com.mythossmp.domain.trial.TrialSession
import com.mythossmp.domain.trial.TrialState
import com.mythossmp.domain.trial.WaveConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TrialService(
    private val plugin: MythosSMP,
    private val scheduler: Scheduler
) {

    private val sessions = ConcurrentHashMap<UUID, TrialSession>()
    private val mobToSession = ConcurrentHashMap<UUID, UUID>()
    private val completedTrials = mutableSetOf<String>()
    private var tickTask: Cancellable? = null
    private val defaultConfigs = mutableMapOf<String, TrialConfig>()

    private companion object {
        const val MAX_CUSTOM_ENTITIES = 25
    }

    fun registerDefaultConfig(config: TrialConfig) {
        defaultConfigs[config.key] = config
    }

    fun getDefaultConfig(key: String): TrialConfig? = defaultConfigs[key]

    fun getAllDefaultConfigs(): Collection<TrialConfig> = defaultConfigs.values

    fun start() {
        tickTask = scheduler.repeat(Duration.ofSeconds(1)) { tick() }
        plugin.logger.info("TrialService started with ${defaultConfigs.size} trial configs")
    }

    fun stop() {
        tickTask?.cancel()
        sessions.values.forEach { cleanupSession(it) }
        sessions.clear()
        mobToSession.clear()
    }

    fun startTrial(initiator: Player, trialKey: String): TrialSession? {
        val config = defaultConfigs[trialKey] ?: run {
            initiator.sendMessage(Component.text("Unknown trial: $trialKey", NamedTextColor.RED))
            return null
        }

        val existing = sessions.values.find { it.isParticipant(initiator.uniqueId) }
        if (existing != null) {
            initiator.sendMessage(Component.text("You are already in a trial!", NamedTextColor.RED))
            return null
        }

        val loc = initiator.location
        val boundary = GeoBoundary(
            worldName = loc.world?.name ?: "world",
            centerX = loc.blockX, centerZ = loc.blockZ,
            radius = 20,
            minY = loc.blockY - 5, maxY = loc.blockY + 10
        )

        val session = TrialSession(
            id = UUID.randomUUID(),
            trialConfig = config,
            initiatorUuid = initiator.uniqueId,
            party = ConcurrentHashMap.newKeySet<UUID>(),
            state = TrialState.SUMMONING,
            currentWave = 0,
            startTime = System.currentTimeMillis(),
            spawnedMobs = ConcurrentHashMap.newKeySet(),
            location = loc.clone(),
            boundary = boundary,
            waveStartTime = System.currentTimeMillis()
        )

        sessions[session.id] = session

        announceToParty(session, Component.text()
            .append(Component.text("\uD83D\uDCDC ", NamedTextColor.GOLD))
            .append(Component.text("Trial of ", NamedTextColor.WHITE))
            .append(Component.text(config.displayName, NamedTextColor.GOLD))
            .append(Component.text(" begins!", NamedTextColor.WHITE))
            .build()
        )

        broadcastCoords(session)
        startWave(session)
        return session
    }

    private fun startWave(session: TrialSession) {
        session.currentWave++
        session.waveStartTime = System.currentTimeMillis()
        if (session.state == TrialState.SUMMONING) {
            session.transitionTo(TrialState.ACTIVE)
        }

        val wave = session.trialConfig.waves.getOrNull(session.currentWave - 1) ?: run {
            completeTrial(session)
            return
        }

        val partySize = session.allPlayers().size
        session.waveMobsRemaining = 0

        for (entry in wave.mobs) {
            val count = if (entry.scaled) {
                (entry.count * (1.0 + (partySize - 1) * 0.3)).toInt().coerceAtLeast(entry.count)
            } else entry.count

            session.waveMobsRemaining += count
            repeat(count) { spawnMob(session, entry.entityType, partySize, session.currentWave) }
        }

        announceToParty(session, Component.text()
            .append(Component.text("\u2694 Wave ${session.currentWave}/${session.trialConfig.totalWaves}", NamedTextColor.RED))
            .append(Component.text(" — ${session.waveMobsRemaining} enemies spawned!", NamedTextColor.GRAY))
            .build()
        )
    }

    private fun spawnMob(session: TrialSession, type: EntityType, partySize: Int, waveNumber: Int) {
        val world = session.location.world ?: return
        val b = session.boundary

        val scanCenter = Location(world, b.centerX + 0.5, b.centerY() + 0.5, b.centerZ + 0.5)
        val scanExtent = b.radius + 2.0
        val scanHeight = ((b.maxY - b.minY) / 2.0) + 2.0
        val activeNearby = world.getNearbyEntities(scanCenter, scanExtent, scanHeight, scanExtent)
            .count { isTrialMob(it.uniqueId) }
        if (activeNearby >= MAX_CUSTOM_ENTITIES) return

        val (x, z) = b.randomSpawnXZ()
        val y = world.getHighestBlockYAt(x, z).coerceIn(b.minY, b.maxY)

        val loc = Location(world, x + 0.5, y + 1.0, z + 0.5)
        val entity = world.spawnEntity(loc, type) as? LivingEntity ?: return

        val baseHealth = session.trialConfig.baseHealth
        val baseDamage = session.trialConfig.baseDamage
        val health = MobScalingFormula.calculateHealth(baseHealth, partySize, waveNumber)
        val damage = MobScalingFormula.calculateDamage(baseDamage, partySize, waveNumber)

        val attrMaxHealth = entity.getAttribute(Attribute.MAX_HEALTH) ?: return
        attrMaxHealth.baseValue = health
        entity.health = health

        val attrDamage = entity.getAttribute(Attribute.ATTACK_DAMAGE)
        attrDamage?.baseValue = damage

        entity.customName(Component.text()
            .append(Component.text("\uD83D\uDC0D ${type.name} ", NamedTextColor.RED))
            .append(Component.text("Lv.${waveNumber}", NamedTextColor.GRAY))
            .build()
        )
        entity.isCustomNameVisible = true

        session.spawnedMobs.add(entity.uniqueId)
        mobToSession[entity.uniqueId] = session.id
    }

    fun onMobDeath(entityUuid: UUID) {
        val sessionId = mobToSession.remove(entityUuid) ?: return
        val session = sessions[sessionId] ?: return

        session.spawnedMobs.remove(entityUuid)
        session.waveMobsRemaining--

        if (session.waveMobsRemaining <= 0 && session.state == TrialState.ACTIVE) {
            advanceWave(session)
        }
    }

    private fun advanceWave(session: TrialSession) {
        if (session.currentWave >= session.trialConfig.totalWaves) {
            completeTrial(session)
            return
        }
        startWave(session)
    }

    private fun completeTrial(session: TrialSession) {
        session.transitionTo(TrialState.COMPLETED)

        completedTrials.add("${session.initiatorUuid}:${session.trialConfig.key}")
        session.party.forEach { uuid ->
            completedTrials.add("$uuid:${session.trialConfig.key}")
        }

        plugin.server.pluginManager.callEvent(TrialCompletedEvent(
            playerUuid = session.initiatorUuid,
            trialKey = session.trialConfig.key,
            displayName = session.trialConfig.displayName
        ))

        announceToParty(session, Component.text()
            .append(Component.text("\u2728 ", NamedTextColor.GOLD))
            .append(Component.text("Trial Complete!", NamedTextColor.GREEN))
            .build()
        )

        val initiator = plugin.server.getPlayer(session.initiatorUuid)
        if (initiator != null) {
            val title = Title.title(
                Component.text("\u2728 Victory", NamedTextColor.GOLD),
                Component.text("You may now accept the ${session.trialConfig.displayName} vow.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
            )
            initiator.showTitle(title)
        }

        cleanupSession(session)
        sessions.remove(session.id)
    }

    private fun failTrial(session: TrialSession, reason: String) {
        if (session.state != TrialState.ACTIVE && session.state != TrialState.SUMMONING) return
        session.transitionTo(TrialState.FAILED)

        announceToParty(session, Component.text()
            .append(Component.text("\u2620 ", NamedTextColor.RED))
            .append(Component.text("Trial Failed: ", NamedTextColor.RED))
            .append(Component.text(reason, NamedTextColor.GRAY))
            .build()
        )

        cleanupSession(session)
        sessions.remove(session.id)
    }

    private fun cleanupSession(session: TrialSession) {
        val world = session.location.world ?: return
        session.spawnedMobs.forEach { uuid ->
            val entity = world.getEntity(uuid)
            if (entity != null && !entity.isDead) entity.remove()
            mobToSession.remove(uuid)
        }
        session.spawnedMobs.clear()
    }

    fun isTrialMob(entityUuid: UUID): Boolean = mobToSession.containsKey(entityUuid)

    fun isParticipating(playerUuid: UUID): Boolean =
        sessions.values.any { it.isParticipant(playerUuid) }

    fun getSessionForPlayer(playerUuid: UUID): TrialSession? =
        sessions.values.find { it.isParticipant(playerUuid) }

    fun hasCompletedTrial(playerUuid: UUID, trialKey: String): Boolean =
        completedTrials.contains("$playerUuid:$trialKey")

    fun addPartyMember(player: Player, targetUuid: UUID): Boolean {
        val session = getSessionForPlayer(player.uniqueId) ?: return false
        if (session.state != TrialState.SUMMONING) return false
        if (session.party.size >= session.trialConfig.maxPartySize) return false
        if (session.isParticipant(targetUuid)) return false
        session.party.add(targetUuid)
        return true
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        sessions.entries.removeIf { (_, session) ->
            if (session.state == TrialState.COMPLETED || session.state == TrialState.FAILED) {
                true
            } else {
                tickSession(session, now)
                false
            }
        }
    }

    private fun tickSession(session: TrialSession, now: Long) {
        val elapsed = (now - session.startTime) / 1000

        if (elapsed > session.trialConfig.timeLimitSeconds) {
            failTrial(session, "Time limit exceeded.")
            return
        }

        val allOnline = session.allPlayers().all { uuid ->
            val p = plugin.server.getPlayer(uuid)
            p != null && p.isOnline
        }
        if (!allOnline) {
            failTrial(session, "A participant disconnected.")
            return
        }

        val anyAlive = session.allPlayers().any { uuid ->
            val p = plugin.server.getPlayer(uuid)
            p != null && !p.isDead
        }
        if (!anyAlive) {
            failTrial(session, "All participants have fallen.")
            return
        }

        checkBoundaries(session)

        if (elapsed % 2 == 0L) {
            showBoundaryParticles(session)
        }

        if (elapsed % 10 == 0L) {
            broadcastCoords(session)
        }

        if (session.state == TrialState.ACTIVE) {
            val waveTimeout = session.trialConfig.waves
                .getOrNull(session.currentWave - 1)?.timeoutSeconds ?: 120
            if (session.waveElapsedSeconds > waveTimeout && session.waveMobsRemaining > 0) {
                announceToParty(session, Component.text()
                    .append(Component.text("\u26A0 Wave timeout! Remaining mobs: ${session.waveMobsRemaining}", NamedTextColor.YELLOW))
                    .build()
                )
                session.waveMobsRemaining = 0
                advanceWave(session)
            }
        }
    }

    private fun checkBoundaries(session: TrialSession) {
        val world = session.location.world
        for (uuid in session.allPlayers()) {
            val player = plugin.server.getPlayer(uuid) ?: continue
            if (!player.isOnline) continue
            if (session.boundary.contains(player.location)) continue

            player.sendMessage(Component.text("\u26A0 You are leaving the trial area!", NamedTextColor.RED))

            val cx = session.boundary.centerX
            val cz = session.boundary.centerZ
            val safeY = world?.getHighestBlockYAt(cx, cz)?.coerceAtLeast(session.boundary.minY) ?: session.boundary.minY
            val center = Location(world ?: player.world, cx + 0.5, safeY.toDouble(), cz + 0.5)
            player.teleport(center)
        }
        if (world == null) return
        for (mobUuid in session.spawnedMobs) {
            val entity = world.getEntity(mobUuid) as? LivingEntity ?: continue
            if (session.boundary.contains(entity.location)) continue
            val (x, z) = session.boundary.randomSpawnXZ()
            val y = world.getHighestBlockYAt(x, z).coerceIn(session.boundary.minY, session.boundary.maxY)
            entity.teleport(Location(world, x + 0.5, y + 1.0, z + 0.5))
        }
    }

    private fun broadcastCoords(session: TrialSession) {
        val cx = session.boundary.centerX
        val cy = session.boundary.centerY()
        val cz = session.boundary.centerZ
        val msg = Component.text()
            .append(Component.text("\uD83D\uDCCD Trial Center: ", NamedTextColor.GOLD))
            .append(Component.text("$cx, $cy, $cz", NamedTextColor.WHITE))
            .append(Component.text("  Wave: ${session.currentWave}/${session.trialConfig.totalWaves}", NamedTextColor.GRAY))
            .build()

        for (uuid in session.allPlayers()) {
            val player = plugin.server.getPlayer(uuid) ?: continue
            player.sendActionBar(msg)
        }
    }

    private fun showBoundaryParticles(session: TrialSession) {
        val world = session.location.world ?: return
        val b = session.boundary
        for ((x, z) in b.circumferencePoints(24)) {
            world.spawnParticle(Particle.SCULK_SOUL, x, b.minY + 0.5, z, 1, 0.0, 0.0, 0.0, 0.01)
            world.spawnParticle(Particle.SCULK_SOUL, x, b.maxY + 0.5, z, 1, 0.0, 0.0, 0.0, 0.01)
        }
    }

    private fun announceToParty(session: TrialSession, message: Component) {
        for (uuid in session.allPlayers()) {
            val player = plugin.server.getPlayer(uuid) ?: continue
            player.sendMessage(message)
        }
    }
}
