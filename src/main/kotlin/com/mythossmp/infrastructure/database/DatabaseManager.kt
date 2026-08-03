package com.mythossmp.infrastructure.database

import com.mythossmp.MythosSMP
import com.mythossmp.infrastructure.config.ConfigManager
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.sql.Connection
import java.sql.SQLException

class DatabaseManager(
    private val plugin: MythosSMP,
    private val configManager: ConfigManager
) {

    private var dataSource: HikariDataSource? = null
    private var isShutdown = false

    fun initialize() {
        val dbPath = configManager.getDatabasePath()
        val poolSize = configManager.getPoolSize()

        val dbFile = File(dbPath)
        dbFile.parentFile?.mkdirs()

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:$dbPath"
            driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = poolSize
            minimumIdle = 1
            idleTimeout = 30000
            maxLifetime = 60000
            connectionTimeout = 5000
            poolName = "MythosSMP-Pool"
            connectionInitSql = "PRAGMA foreign_keys=ON"
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
                executeUpdate(TableSchema.COMPLETED_TRIALS_TABLE)
                executeUpdate(TableSchema.PLAYER_INDEX)
                executeUpdate(TableSchema.TRUST_INDEX)
                executeUpdate(TableSchema.UNION_INDEX)
                close()
            }
        }
        migrateSchema()
    }

    private fun migrateSchema() {
        getConnection().use { conn ->
            val columns = mutableSetOf<String>()
            conn.createStatement().use { st ->
                st.executeQuery("PRAGMA table_info(mythos_players)").use { rs ->
                    while (rs.next()) {
                        columns.add(rs.getString("name"))
                    }
                }
            }
            conn.createStatement().use { st ->
                if ("ares_rage" !in columns) {
                    st.executeUpdate("ALTER TABLE mythos_players ADD COLUMN ares_rage INTEGER DEFAULT 0")
                }
                if ("hades_charges" !in columns) {
                    st.executeUpdate("ALTER TABLE mythos_players ADD COLUMN hades_charges INTEGER DEFAULT 0")
                }
                if ("zeus_charge" !in columns) {
                    st.executeUpdate("ALTER TABLE mythos_players ADD COLUMN zeus_charge REAL DEFAULT 0.0")
                }
            }
        }
    }

    fun getConnection(): Connection {
        if (isShutdown) throw SQLException("DataSource has been shut down")
        val conn = dataSource?.connection
            ?: throw SQLException("DataSource not initialized")
        conn.createStatement().use { it.execute("PRAGMA foreign_keys=ON") }
        return conn
    }

    fun shutdown() {
        isShutdown = true
        dataSource?.close()
        dataSource = null
    }
}
