package com.mythossmp.application.combat

import com.mythossmp.application.trust.TrustService
import com.mythossmp.application.union.UnionService
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.projectiles.ProjectileSource

class CombatService(
    private val trustService: TrustService,
    private val unionService: UnionService
) {

    fun shouldCancelDamage(damagerEntity: Entity, damagedEntity: Entity): Boolean {
        val damaged = damagedEntity as? Player ?: return false
        val damager = resolvePlayerDamager(damagerEntity) ?: return false
        if (damager.uniqueId == damaged.uniqueId) return false
        return trustService.areMutuallyTrusted(damager.uniqueId, damaged.uniqueId)
            || unionService.areInSameUnion(damager.uniqueId, damaged.uniqueId)
    }

    fun shouldCancelAreaEffect(sourceEntity: Entity, affectedEntity: Entity): Boolean {
        val affected = affectedEntity as? Player ?: return false
        val source = resolveAreaEffectSource(sourceEntity) ?: return false
        if (source.uniqueId == affected.uniqueId) return false
        return trustService.areMutuallyTrusted(source.uniqueId, affected.uniqueId)
            || unionService.areInSameUnion(source.uniqueId, affected.uniqueId)
    }

    private fun resolvePlayerDamager(entity: Entity): Player? {
        return when (entity) {
            is Player -> entity
            is Projectile -> {
                val shooter = entity.shooter
                if (shooter is Player) shooter else null
            }
            else -> null
        }
    }

    private fun resolveAreaEffectSource(entity: Entity): Player? {
        return when (entity) {
            is AreaEffectCloud -> {
                val source = entity.source
                if (source is Player) source else null
            }
            is Projectile -> {
                val shooter = entity.shooter
                if (shooter is Player) shooter else null
            }
            else -> null
        }
    }
}
