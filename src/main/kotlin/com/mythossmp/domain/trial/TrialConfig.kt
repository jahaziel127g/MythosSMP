package com.mythossmp.domain.trial

import org.bukkit.entity.EntityType

data class TrialConfig(
    val key: String,
    val displayName: String,
    val vowKey: String,
    val description: String,
    val waves: List<WaveConfig>,
    val baseHealth: Double,
    val baseDamage: Double,
    val timeLimitSeconds: Int,
    val maxPartySize: Int,
    val respawnRadius: Double = 5.0
) {
    val totalWaves: Int get() = waves.size
}

data class WaveConfig(
    val waveNumber: Int,
    val mobs: List<MobSpawnEntry>,
    val timeoutSeconds: Int = 120
)

data class MobSpawnEntry(
    val entityType: EntityType,
    val count: Int,
    val scaled: Boolean = true
)
