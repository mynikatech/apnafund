package com.mynikatech.apnafund.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin

private fun env(name: String, default: String? = null): String =
    System.getenv(name) ?: default ?: error("$name not set")

fun shutdownDb() {
    Db.dataSource.close()
    Db.aiDataSource.close()
}

object Db {

    val dataSource by lazy {
        createDataSource(
            env("DB_USER"),
            env("DB_PASSWORD"),
            env("DB_SCHEMA")
        )
    }
    val aiDataSource by lazy {
        createDataSource(
            env("AI_DB_USER", env("DB_USER")),
            env("AI_DB_PASSWORD", env("DB_PASSWORD")),
            env("AI_DB_SCHEMA")
        )
    }

    private fun createDataSource(
        user: String,
        password: String,
        schema: String
    ): HikariDataSource {

        val host = env("DB_HOST")
        val port = env("DB_PORT")
        val db = env("DB_NAME")

        return HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://$host:$port/$db"
            username = user
            this.password = password

            connectionInitSql = """SET search_path TO "$schema", public"""

            maximumPoolSize = env("DB_POOL_SIZE", "10").toInt()
            minimumIdle = env("DB_MIN_IDLE", "1").toInt()
            connectionTimeout = env("DB_CONNECT_TIMEOUT_MS", "30000").toLong()
            idleTimeout = env("DB_IDLE_TIMEOUT_MS", "600000").toLong()
            maxLifetime = env("DB_MAX_LIFETIME_MS", "1800000").toLong()

            driverClassName = "org.postgresql.Driver"

            addDataSourceProperty("ApplicationName", "apnafund-server")
            addDataSourceProperty("reWriteBatchedInserts", "true")
        }.let(::HikariDataSource)
    }

    private fun createJdbi(ds: HikariDataSource): Jdbi =
        Jdbi.create(ds)
            .installPlugin(KotlinPlugin())
            .installPlugin(SqlObjectPlugin())

    val jdbi by lazy {
        createJdbi(dataSource)
    }

    val aiJdbi by lazy {
        createJdbi(aiDataSource)
    }
}