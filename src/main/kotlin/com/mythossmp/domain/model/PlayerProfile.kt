package com.mythossmp.domain.model

import java.util.UUID

data class PlayerProfile(
    val playerUuid: UUID,
    val lastKnownName: String,
    val activeVow: String? = null,
    val cooldownMultiplier: Double = 1.0,
    val dirty: Boolean = false,
    val aresRage: Int = 0,
    val hadesCharges: Int = 0,
    val zeusCharge: Double = 0.0
)
