package com.mythossmp.player

import java.util.UUID

data class PlayerProfile(
    val playerUuid: UUID,
    var lastKnownName: String,
    var activeVow: String? = null,
    var cooldownMultiplier: Double = 1.0,
    var dirty: Boolean = false
)
