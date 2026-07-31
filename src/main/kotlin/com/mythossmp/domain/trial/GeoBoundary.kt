package com.mythossmp.domain.trial

import org.bukkit.Location
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoBoundary(
    val worldName: String,
    val centerX: Int, val centerZ: Int,
    val radius: Int,
    val minY: Int, val maxY: Int
) {
    init {
        require(radius > 0) { "radius must be positive" }
        require(minY <= maxY) { "minY ($minY) must be <= maxY ($maxY)" }
    }

    fun centerY(): Int = (minY + maxY) / 2

    fun contains(location: Location): Boolean {
        if (location.world?.name != worldName) return false
        val dx = location.blockX - centerX
        val dz = location.blockZ - centerZ
        val y = location.blockY
        return dx.toLong() * dx + dz.toLong() * dz <= radius.toLong() * radius && y in minY..maxY
    }

    fun randomSpawnXZ(): Pair<Int, Int> {
        val angle = Math.random() * 2 * Math.PI
        val dist = sqrt(Math.random()) * radius
        val x = centerX + (dist * cos(angle)).roundToInt()
        val z = centerZ + (dist * sin(angle)).roundToInt()
        return x to z
    }

    fun circumferencePoints(count: Int): List<Pair<Double, Double>> {
        return (0 until count).map { i ->
            val angle = (2 * Math.PI * i) / count
            val x = centerX + radius * cos(angle)
            val z = centerZ + radius * sin(angle)
            x to z
        }
    }
}
