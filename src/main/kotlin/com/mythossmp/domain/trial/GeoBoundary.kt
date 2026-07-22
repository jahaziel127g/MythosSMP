package com.mythossmp.domain.trial

import org.bukkit.Location
import kotlin.math.roundToInt

data class GeoBoundary(
    val worldName: String,
    val minX: Int, val minY: Int, val minZ: Int,
    val maxX: Int, val maxY: Int, val maxZ: Int
) {
    init {
        require(minX <= maxX) { "minX ($minX) must be <= maxX ($maxX)" }
        require(minY <= maxY) { "minY ($minY) must be <= maxY ($maxY)" }
        require(minZ <= maxZ) { "minZ ($minZ) must be <= maxZ ($maxZ)" }
    }

    fun contains(location: Location): Boolean {
        if (location.world?.name != worldName) return false
        val x = location.blockX; val y = location.blockY; val z = location.blockZ
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }

    fun centerX(): Int = (minX + maxX) / 2
    fun centerY(): Int = (minY + maxY) / 2
    fun centerZ(): Int = (minZ + maxZ) / 2

    fun sizeX(): Int = maxX - minX + 1
    fun sizeY(): Int = maxY - minY + 1
    fun sizeZ(): Int = maxZ - minZ + 1
}
