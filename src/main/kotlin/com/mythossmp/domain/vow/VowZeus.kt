package com.mythossmp.domain.vow

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VowZeus {

    private val chargeRegistry = ConcurrentHashMap<UUID, Double>()

    fun getCharge(uuid: UUID): Double = chargeRegistry.getOrDefault(uuid, 0.0)

    fun addCharge(uuid: UUID, amount: Double) {
        chargeRegistry.compute(uuid) { _, current ->
            ((current ?: 0.0) + amount.coerceAtLeast(0.0)).coerceAtMost(1.0)
        }
    }

    fun consumeCharge(uuid: UUID): Boolean {
        return if (getCharge(uuid) >= 1.0) {
            chargeRegistry[uuid] = 0.0
            true
        } else {
            false
        }
    }
}
