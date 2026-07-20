package com.mythossmp.model

interface Vow {
    val key: String
    val displayName: String
    val enabled: Boolean
    val description: String
    val passiveEffects: List<PassiveEffect>
    val ability1: ActiveAbility
    val ability2: ActiveAbility
    val curse: Curse
}
