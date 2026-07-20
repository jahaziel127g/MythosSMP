package com.mythossmp.infrastructure.config

import com.mythossmp.MythosSMP
import org.bukkit.configuration.file.FileConfiguration

class ConfigManager(private val plugin: MythosSMP) {

    private var config: FileConfiguration = plugin.config

    fun load() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        config = plugin.config
    }

    fun getDatabasePath(): String {
        return config.getString("database.path") ?: "${plugin.dataFolder.absolutePath}/mythos_smp.db"
    }

    fun getPoolSize(): Int {
        val size = config.getInt("database.pool-size", 4)
        if (size <= 0) {
            plugin.logger.warning("Invalid database.pool-size ($size); falling back to 4.")
            return 4
        }
        return size
    }

    fun getMaxBackups(): Int {
        return config.getInt("database.max-backups", 5)
    }

    fun isDebugMode(): Boolean {
        return config.getBoolean("debug", false)
    }
}
