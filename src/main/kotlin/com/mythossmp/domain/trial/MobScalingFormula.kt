package com.mythossmp.domain.trial

object MobScalingFormula {

    fun calculateHealth(baseHealth: Double, partySize: Int, waveNumber: Int): Double {
        val partyMult = 1.0 + (partySize - 1).coerceAtLeast(0) * 0.5
        val waveMult = 1.0 + (waveNumber - 1) * 0.35
        return baseHealth * partyMult * waveMult
    }

    fun calculateDamage(baseDamage: Double, partySize: Int, waveNumber: Int): Double {
        val partyMult = 1.0 + (partySize - 1).coerceAtLeast(0) * 0.3
        val waveMult = 1.0 + (waveNumber - 1) * 0.2
        return baseDamage * partyMult * waveMult
    }

    fun calculateExperience(baseXp: Int, waveNumber: Int): Int {
        return (baseXp * (1.0 + (waveNumber - 1) * 0.25)).roundToInt()
    }

    private fun Double.roundToInt(): Int = (this + 0.5).toInt()
}
