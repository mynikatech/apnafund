package com.mynikatech.apnafund.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin

private fun env(name: String, default: String? = null): String =
    System.getenv(name) ?: default ?: error("$name not set")

fun shutdownDb() = Db.dataSource.close()

object Db {
    val dataSource: HikariDataSource by lazy {
        val host = env("DB_HOST")
        val port = env("DB_PORT")
        val db = env("DB_NAME")
        val user = env("DB_USER")
        val pass = env("DB_PASSWORD")
        val schema = env("DB_SCHEMA")

        HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://$host:$port/$db"
            username = user
            password = pass

            // Critical: force the schema per connection (mixed-case needs quotes)
            connectionInitSql = """SET search_path TO "$schema", public"""

            // Pool tuning (override via env if you like)
            maximumPoolSize = env("DB_POOL_SIZE", "10").toInt()
            minimumIdle = env("DB_MIN_IDLE", "1").toInt()
            connectionTimeout = env("DB_CONNECT_TIMEOUT_MS", "30000").toLong()
            idleTimeout = env("DB_IDLE_TIMEOUT_MS", "600000").toLong()
            maxLifetime = env("DB_MAX_LIFETIME_MS", "1800000").toLong()

            driverClassName = "org.postgresql.Driver"

            // Useful PG niceties (optional)
            addDataSourceProperty("ApplicationName", "apnabank-server")
            addDataSourceProperty("reWriteBatchedInserts", "true")
        }.let(::HikariDataSource)
    }

    val jdbi: Jdbi by lazy {
        Jdbi.create(dataSource)
            .installPlugin(KotlinPlugin())
            .installPlugin(SqlObjectPlugin())
    }
}