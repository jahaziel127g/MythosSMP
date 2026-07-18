package com.mythossmp.model

import org.bukkit.entity.Player

interface ActiveAbility {
    val key: String
    val displayName: String
    val cooldownSeconds: Int
    val description: String

    fun execute(player: Player, plugin: Any)
}

interface PassiveEffect {
    val key: String
    val description: String

    fun apply(player: Player)
    fun remove(player: Player)
}

interface Curse {
    val key: String
    val description: String

    fun apply(player: Player)
    fun remove(player: Player)
}
