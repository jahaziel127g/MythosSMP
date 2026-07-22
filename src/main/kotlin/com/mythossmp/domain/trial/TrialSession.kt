package com.mythossmp.domain.trial

import org.bukkit.Location
import java.util.UUID

data class TrialSession(
    val id: UUID,
    val trialConfig: TrialConfig,
    val initiatorUuid: UUID,
    val party: MutableSet<UUID>,
    var state: TrialState,
    var currentWave: Int,
    val startTime: Long,
    val spawnedMobs: MutableSet<UUID>,
    val location: Location,
    val boundary: GeoBoundary,
    var waveStartTime: Long,
    var waveMobsRemaining: Int = 0
) {
    val elapsedSeconds: Long get() = (System.currentTimeMillis() - startTime) / 1000
    val waveElapsedSeconds: Long get() = (System.currentTimeMillis() - waveStartTime) / 1000

    fun allPlayers(): Set<UUID> = party + initiatorUuid

    fun isParticipant(uuid: UUID): Boolean = uuid == initiatorUuid || uuid in party

    fun canTransitionTo(next: TrialState): Boolean = state.canTransitionTo(next)

    fun transitionTo(next: TrialState) {
        require(canTransitionTo(next)) { "Cannot transition from $state to $next" }
        state = next
    }
}
