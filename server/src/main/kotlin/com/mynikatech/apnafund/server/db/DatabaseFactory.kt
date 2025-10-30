package com.mynikatech.apnafund.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger

object DatabaseFactory {
    private fun dataSource(): HikariDataSource {
        val url  = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/ApnaFund"
        val user = System.getenv("DB_USER") ?: "postgres"
        val pass = System.getenv("DB_PASSWORD") ?: "admin@123"

        val cfg = HikariConfig().apply {
            jdbcUrl = url
            username = user
            password = pass
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
        return HikariDataSource(cfg)
    }

    fun init() {
        val ds = dataSource()
/*
        try {
            // If you haven’t added any migrations yet, this will just do nothing.
            val flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
            val result = flyway.migrate()
            println("Flyway: applied ${result.migrationsExecuted} migration(s)")
        } catch (e: Exception) {
            System.err.println("Flyway failed: ${e.message}")
            e.printStackTrace()
            throw e
        }*/

        try {
            Database.connect(ds)
            transaction {
                // Optional: log first statements
                addLogger(StdOutSqlLogger)
                exec("SELECT 1") { rs ->
                    if (rs.next()) println("DB connection OK (SELECT 1 -> ${rs.getInt(1)})")
                }
            }
        } catch (e: Exception) {
            System.err.println("Database connect/test failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
