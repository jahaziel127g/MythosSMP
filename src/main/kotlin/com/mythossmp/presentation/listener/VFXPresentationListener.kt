package com.mythossmp.presentation.listener

import com.mythossmp.domain.event.VowVFXEvent
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class VFXPresentationListener : Listener {

    @EventHandler
    fun onVowVFXEvent(event: VowVFXEvent) {
        val world = event.location.world ?: return

        when (event) {
            is VowVFXEvent.AresBloodthorns -> {
                world.spawnParticle(
                    Particle.DUST,
                    event.location.add(0.0, 1.0, 0.0),
                    50, 0.5, 0.5, 0.5, 0.1,
                    Particle.DustOptions(Color.fromRGB(0xFF0000), 1.5f)
                )
            }

            is VowVFXEvent.AresRageAura -> {
                world.spawnParticle(
                    Particle.DUST,
                    event.location,
                    200, event.radius, 0.5, event.radius, 0.2,
                    Particle.DustOptions(Color.fromRGB(0xFF0000), 2.0f)
                )
            }

            is VowVFXEvent.LokiBackstab -> {
                world.spawnParticle(
                    Particle.SWEEP_ATTACK,
                    event.location.add(0.0, 1.0, 0.0),
                    1, 0.3, 0.3, 0.3, 0.0
                )
                world.spawnParticle(
                    Particle.DUST,
                    event.location.add(0.0, 1.0, 0.0),
                    15, 0.4, 0.4, 0.4, 0.05,
                    Particle.DustOptions(Color.fromRGB(0x4A0080), 1.2f)
                )
            }

            is VowVFXEvent.LokiShadowStep -> {
                world.spawnParticle(
                    Particle.DUST,
                    event.location.add(0.0, 0.5, 0.0),
                    30, 0.4, 0.2, 0.4, 0.05,
                    Particle.DustOptions(Color.fromRGB(0x4A0080), 1.5f)
                )
            }

            is VowVFXEvent.LokiVeilActivate -> {
                world.spawnParticle(
                    Particle.DUST,
                    event.location.add(0.0, 1.0, 0.0),
                    40, 0.5, 0.5, 0.5, 0.1,
                    Particle.DustOptions(Color.fromRGB(0x9932CC), 1.8f)
                )
            }

            is VowVFXEvent.LokiVeilFirstHit -> {
                world.spawnParticle(
                    Particle.DUST,
                    event.location.add(0.0, 1.0, 0.0),
                    25, 0.5, 0.5, 0.5, 0.1,
                    Particle.DustOptions(Color.fromRGB(0x9932CC), 2.0f)
                )
            }

            is VowVFXEvent.HadesSoulHarvest -> {
                world.spawnParticle(
                    Particle.SCULK_SOUL,
                    event.location.add(0.0, 1.0, 0.0),
                    10, 0.3, 0.3, 0.3, 0.02
                )
            }

            is VowVFXEvent.HadesSoulDamage -> {
                world.spawnParticle(
                    Particle.SCULK_SOUL,
                    event.location.add(0.0, 1.0, 0.0),
                    5, 0.2, 0.2, 0.2, 0.01
                )
            }

            is VowVFXEvent.HadesDeathDefianceArm -> {
                world.spawnParticle(
                    Particle.SCULK_SOUL,
                    event.location.add(0.0, 1.0, 0.0),
                    30, 0.5, 0.5, 0.5, 0.05
                )
            }

            is VowVFXEvent.HadesDeathDefianceSave -> {
                world.spawnParticle(
                    Particle.SOUL,
                    event.location.add(0.0, 1.0, 0.0),
                    50, 0.5, 0.5, 0.5, 0.1
                )
                world.spawnParticle(
                    Particle.DUST,
                    event.location.add(0.0, 1.0, 0.0),
                    30, 0.5, 0.5, 0.5, 0.05,
                    Particle.DustOptions(Color.fromRGB(0x2D0040), 2.0f)
                )
            }

            is VowVFXEvent.HadesSoulBurst -> {
                world.spawnParticle(
                    Particle.SOUL,
                    event.location,
                    100, event.radius, 0.5, event.radius, 0.1
                )
                world.spawnParticle(
                    Particle.SCULK_SOUL,
                    event.location,
                    50, event.radius, 0.5, event.radius, 0.05
                )
            }

            is VowVFXEvent.ZeusStaticOverchargeVFXEvent -> {
                world.strikeLightningEffect(event.location)
            }

            is VowVFXEvent.VowSelectionActivate -> {
                val color = when (event.vowKey) {
                    "ares" -> Color.fromRGB(0xFF0000)
                    "loki" -> Color.fromRGB(0x4A0080)
                    "hades" -> Color.fromRGB(0x00AA00)
                    else -> Color.fromRGB(0xFFFFFF)
                }
                world.spawnParticle(
                    Particle.DUST,
                    event.location.add(0.0, 2.0, 0.0),
                    80, 1.0, 1.0, 1.0, 0.3,
                    Particle.DustOptions(color, 2.5f)
                )
                world.spawnParticle(
                    Particle.END_ROD,
                    event.location.add(0.0, 1.0, 0.0),
                    30, 0.5, 1.0, 0.5, 0.05
                )
            }
        }
    }
}
