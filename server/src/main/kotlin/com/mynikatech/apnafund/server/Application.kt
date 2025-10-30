package com.mynikatech.apnafund.server

import UsersSql
import com.mynikatech.apnafund.server.admin.AdminSql
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.db.Db
import com.mynikatech.apnafund.server.deposits.DepositsSql
import com.mynikatech.apnafund.server.feedback.FeedbackSql
import com.mynikatech.apnafund.server.funds.FundsSql
import com.mynikatech.apnafund.server.groups.GroupsSql
import com.mynikatech.apnafund.server.loans.LoansSql
import com.mynikatech.apnafund.server.notifications.NotificationsSql
import com.mynikatech.apnafund.server.roles.RolesSql
import com.mynikatech.apnafund.server.security.PasswordHistorySql
import com.mynikatech.apnafund.server.security.PinHistorySql
import com.mynikatech.apnafund.server.types.TypesSql
import com.mynikatech.apnafund.server.userroles.UserRolesSql
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.hsts.HSTS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.host
import io.ktor.server.request.uri
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.slf4j.event.Level
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyStore
import java.util.UUID
import kotlin.time.Duration.Companion.days

fun main() {
    val host = System.getenv("HOST") ?: "0.0.0.0"
    val httpPort = System.getenv("HTTP_PORT")?.toIntOrNull() ?: 8080
    val useHttps = (System.getenv("USE_HTTPS") == "1")

    val httpsPort = System.getenv("HTTPS_PORT")?.toIntOrNull() ?: 8443
    val keyStorePathEnv = System.getenv("KEYSTORE_PATH")
    val keyStoreType = System.getenv("KEYSTORE_TYPE") ?: "JKS" // or PKCS12
    val keyAlias = System.getenv("KEY_ALIAS") ?: "selfsigned"
    val keyStorePassword = (System.getenv("KEYSTORE_PASSWORD") ?: "changeit").toCharArray()
    val privateKeyPassword =
        (System.getenv("PRIVATE_KEY_PASSWORD") ?: String(keyStorePassword)).toCharArray()

    embeddedServer(
        Netty,
        configure = {
            // HTTP connector (optional)
            if (httpPort > 0) {
                connector {
                    this.host = host
                    this.port = httpPort
                }
            }

            // HTTPS connector (conditional)
            if (useHttps && !keyStorePathEnv.isNullOrBlank()) {
                val ksPath = Paths.get(keyStorePathEnv)
                require(Files.exists(ksPath)) { "Keystore not found at $ksPath" }

                val keyStore = KeyStore.getInstance(keyStoreType).apply {
                    Files.newInputStream(ksPath).use { load(it, keyStorePassword) }
                }

                sslConnector(
                    keyStore = keyStore,
                    keyAlias = keyAlias,
                    keyStorePassword = { keyStorePassword },
                    privateKeyPassword = { privateKeyPassword },
                ) {
                    this.host = host
                    this.port = httpsPort
                }
            }
        },
        module = Application::module
    ).start(wait = true)
}

/** Minimal HTTPS redirect plugin for Ktor 3 (replaces the removed HttpsRedirect plugin). */
private val HttpsEnforcer = createApplicationPlugin("HttpsEnforcer") {
    val sslPort = System.getenv("HTTPS_PORT")?.toIntOrNull() ?: 8443
    val permanent = true

    onCall { call ->
        // Prefer proxy headers if present; fall back to local scheme.
        val proto = call.request.headers["X-Forwarded-Proto"]
            ?: call.request.headers["X-Forwarded-Protocol"]
            ?: call.request.local.scheme

        if (proto.equals("http", ignoreCase = true)) {
            val forwardedHost = call.request.headers["X-Forwarded-Host"]
            val host = forwardedHost ?: call.request.host()
            val uri = call.request.uri
            val hostHasPort = ':' in host
            val portSuffix = if (sslPort == 443 || hostHasPort) "" else ":$sslPort"

            val target = "https://$host$portSuffix$uri"
            call.respondRedirect(target, permanent)
            return@onCall
        }
    }
}

fun Application.module() {
    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString().replace("-", "") }
        verify { it.isNotBlank() }
        replyToHeader(HttpHeaders.XRequestId)
    }

    install(ContentNegotiation) { json() }
    install(CallLogging) { level = Level.INFO }

    // Force HTTPS
    val useHttps = System.getenv("USE_HTTPS") == "1"
    if (useHttps) {
        install(HttpsEnforcer)

        // Strict-Transport-Security (Ktor 3 uses seconds)
        install(HSTS) {
            maxAgeInSeconds = 365.days.inWholeSeconds
            includeSubDomains = true
            preload = true
        }
    }

    // Initialize DAOs
    val jdbi = Db.jdbi
    val usersDao = jdbi.onDemand(UsersSql::class.java)
    val deposistsDao = jdbi.onDemand(DepositsSql::class.java)
    val feedbackDao = jdbi.onDemand(FeedbackSql::class.java)
    val groupsDao = jdbi.onDemand(GroupsSql::class.java)
    val roleDao = jdbi.onDemand(RolesSql::class.java)
    val userRolesDao = jdbi.onDemand(UserRolesSql::class.java)
    val fundDao = jdbi.onDemand(FundsSql::class.java)
    val loansDao = jdbi.onDemand(LoansSql::class.java)
    val typeDao = jdbi.onDemand(TypesSql::class.java)
    val notificationsDao = jdbi.onDemand(NotificationsSql::class.java)
    val adminDao = jdbi.onDemand(AdminSql::class.java)
    val passwordHistoryDao = jdbi.onDemand(PasswordHistorySql::class.java)
    val pinHistoryDao = jdbi.onDemand(PinHistorySql::class.java)

    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respondError(HttpStatusCode.NotFound, "not_found", "Resource not found")
        }
        status(HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.respondError(
                HttpStatusCode.MethodNotAllowed,
                "method_not_allowed",
                "Method not allowed"
            )
        }
        // Parse/validation
        exception<io.ktor.server.plugins.BadRequestException> { call, cause ->
            call.respondError(HttpStatusCode.BadRequest, "validation", "Bad request", cause.message)
        }
        // JDBI / SQL
        exception<org.jdbi.v3.core.statement.UnableToExecuteStatementException> { call, cause ->
            call.respondError(
                HttpStatusCode.Conflict, "db_conflict", "Database conflict",
                cause.cause?.message?.take(200)
            )
        }
        exception<java.sql.SQLException> { call, cause ->
            call.respondError(
                HttpStatusCode.InternalServerError, "db_error", "Database error",
                cause.message?.take(200)
            )
        }
        // Last resort
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled", cause)
            call.respondError(
                HttpStatusCode.InternalServerError, "internal", "Something went wrong"
            )
        }
    }

    routing {
        get("/health") { call.respondText("OK") }
        registerRoutes(
            usersDao,
            deposistsDao,
            feedbackDao,
            groupsDao,
            roleDao,
            userRolesDao,
            fundDao,
            loansDao,
            typeDao,
            notificationsDao,
            adminDao,
            pinHistoryDao,
            passwordHistoryDao
        )
    }
}
