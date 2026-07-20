package com.mythossmp.database

import com.mythossmp.MythosSMP
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.SQLException

class DatabaseManager(private val plugin: MythosSMP) {

    private var dataSource: HikariDataSource? = null

    fun initialize() {
        val dbPath = plugin.configManager.getDatabasePath()
        val poolSize = plugin.configManager.getPoolSize()

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:$dbPath"
            driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = poolSize
            minimumIdle = 1
            idleTimeout = 30000
            maxLifetime = 60000
            connectionTimeout = 5000
            poolName = "MythosSMP-Pool"
        }

        dataSource = HikariDataSource(hikariConfig)
        createTables()
        plugin.logger.info("Database initialized at $dbPath")
    }

    private fun createTables() {
        getConnection().use { conn ->
            conn.createStatement().apply {
                executeUpdate(TableSchema.PLAYERS_TABLE)
                executeUpdate(TableSchema.TRUSTS_TABLE)
                executeUpdate(TableSchema.UNIONS_TABLE)
                executeUpdate(TableSchema.PLAYER_INDEX)
                executeUpdate(TableSchema.TRUST_INDEX)
                executeUpdate(TableSchema.UNION_INDEX)
                close()
            }
        }
    }

    fun getConnection(): Connection {
        return dataSource?.connection
            ?: throw SQLException("DataSource not initialized")
    }

    fun shutdown() {
        dataSource?.close()
        dataSource = null
    }
}
