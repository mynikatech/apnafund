package com.mynikatech.apnafund.server

import com.mynikatech.apnafund.net.api.ValidationException
import com.mynikatech.apnafund.net.dto.AIProviderType
import com.mynikatech.apnafund.server.admin.AdminSql
import com.mynikatech.apnafund.server.ai.AIClient
import com.mynikatech.apnafund.server.ai.AIService
import com.mynikatech.apnafund.server.ai.GeminiClient
import com.mynikatech.apnafund.server.ai.OpenAIClient
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.approval.ApprovalService
import com.mynikatech.apnafund.server.approval.ApprovalSql
import com.mynikatech.apnafund.server.common.messaging.dispatch.EventDispatchService
import com.mynikatech.apnafund.server.common.messaging.publishers.SupportMessagingPublisher
import com.mynikatech.apnafund.server.common.messaging.publishers.UserMessagingPublisher
import com.mynikatech.apnafund.server.config.FirebaseAdminProvider
import com.mynikatech.apnafund.server.db.Db
import com.mynikatech.apnafund.server.deposits.DepositsSql
import com.mynikatech.apnafund.server.feedback.FeedbackSql
import com.mynikatech.apnafund.server.funds.FundsSql
import com.mynikatech.apnafund.server.groups.GroupsSql
import com.mynikatech.apnafund.server.loans.LoansSql
import com.mynikatech.apnafund.server.notifications.NotificationService
import com.mynikatech.apnafund.server.notifications.NotificationsSql
import com.mynikatech.apnafund.server.roles.RolesSql
import com.mynikatech.apnafund.server.security.PasswordHistorySql
import com.mynikatech.apnafund.server.security.PinHistorySql
import com.mynikatech.apnafund.server.types.TypesSql
import com.mynikatech.apnafund.server.userroles.UserRolesSql
import com.mynikatech.apnafund.server.users.EmailVerificationService
import com.mynikatech.apnafund.server.users.ModeratorRegistrationService
import com.mynikatech.apnafund.server.users.UserFinanceService
import com.mynikatech.apnafund.server.users.UserManagementService
import com.mynikatech.apnafund.server.users.UsersSql
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
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
    val keyStoreType = System.getenv("KEYSTORE_TYPE") ?: "JKS"
    val keyAlias = System.getenv("KEY_ALIAS") ?: "selfsigned"
    val keyStorePassword = (System.getenv("KEYSTORE_PASSWORD") ?: "changeit").toCharArray()
    val privateKeyPassword =
        (System.getenv("PRIVATE_KEY_PASSWORD") ?: String(keyStorePassword)).toCharArray()
    // MUST be first — before Firebase Admin / Firestore
    System.setProperty(
        "io.grpc.internal.DnsNameResolverProvider.enable_unix_domain_socket",
        "false"
    )

    embeddedServer(
        Netty,
        configure = {
            if (httpPort > 0) {
                connector {
                    this.host = host
                    this.port = httpPort
                }
            }

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

/** HTTPS redirect plugin */
private val HttpsEnforcer = createApplicationPlugin("HttpsEnforcer") {
    val sslPort = System.getenv("HTTPS_PORT")?.toIntOrNull() ?: 8443
    val permanent = true

    onCall { call ->
        val proto = call.request.headers["X-Forwarded-Proto"]
            ?: call.request.headers["X-Forwarded-Protocol"]
            ?: call.request.local.scheme

        if (proto.equals("http", ignoreCase = true)) {
            val forwardedHost = call.request.headers["X-Forwarded-Host"]
            val host = forwardedHost ?: call.request.host()
            val uri = call.request.uri
            val hostHasPort = ':' in host
            val portSuffix = if (sslPort == 443 || hostHasPort) "" else ":$sslPort"

            call.respondRedirect("https://$host$portSuffix$uri", permanent)
        }
    }
}

private fun isEmailVerificationEnabled(): Boolean {
    return when (System.getenv("EMAIL_VERIFICATION_ENABLED")) {
        "0" -> false   // 0 = enabled
        "1" -> true  // 1 = disabled
        null -> true  // safe default → ENABLED
        else -> true
    }
}

fun Application.module() {

    println(">>> MODULE STARTED <<<")
    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString().replace("-", "") }
        verify { it.isNotBlank() }
        replyToHeader(HttpHeaders.XRequestId)
    }
    FirebaseAdminProvider.init()
    install(ContentNegotiation) { json() }
    install(CallLogging) { level = Level.INFO }

    if (System.getenv("USE_HTTPS") == "1") {
        install(HttpsEnforcer)
        install(HSTS) {
            maxAgeInSeconds = 365.days.inWholeSeconds
            includeSubDomains = true
            preload = true
        }
    }
    // ------------------ DAOs ------------------
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
    val approvalDao = jdbi.onDemand(ApprovalSql::class.java)

    // ------------------ Messaging ------------------
    val userEventsArn = System.getenv("USER_EVENTS_TOPIC_ARN")
        ?: error("USER_EVENTS_TOPIC_ARN env var not set")

    val supportEventsArn = System.getenv("SUPPORT_EVENTS_TOPIC_ARN")
        ?: error("SUPPORT_EVENTS_TOPIC_ARN env var not set")

    val apiKey = System.getenv("OPENAI_API_KEY")
        ?: throw IllegalStateException("OPENAI_API_KEY not found in environment")

    val geminiKey = System.getenv("GEMINI_API_KEY")
        ?: throw IllegalStateException("GEMINI_API_KEY not found in environment")

    val eventDispatchService = EventDispatchService(
        UserMessagingPublisher(userEventsArn),
        SupportMessagingPublisher(supportEventsArn)
    )
    val emailVerificationEnabled: Boolean = isEmailVerificationEnabled()
    val emailOtpExpiryMinutes =
        System.getenv("EMAIL_OTP_EXPIRY_MINUTES")?.toLongOrNull() ?: 60
    val emailVerificationService =
        EmailVerificationService(usersDao, eventDispatchService, emailOtpExpiryMinutes)
    val moderatorRegistrationService = ModeratorRegistrationService(
        usersDao,
        userRolesDao,
        roleDao,
        groupsDao,
        emailVerificationService,
        eventDispatchService,
        emailVerificationEnabled,
        passwordHistoryDao,
        approvalDao
    )
    val userManagementService = UserManagementService(
        usersDao,
        userRolesDao,
        roleDao,
        groupsDao,
        eventDispatchService,
        emailVerificationService,
        emailVerificationEnabled,
        passwordHistoryDao
    )
    val openAIClient = OpenAIClient(apiKey)
    val aiClient = AIClient(
        openAIClient = null,                 // disable OpenAI
        geminiClient = GeminiClient(geminiKey),
        provider = AIProviderType.GEMINI,
        enableFallback = false               // no fallback needed
    )
    val userFinancialService = UserFinanceService(usersDao,fundDao, loansDao)
    val aiService = AIService(userFinancialService,aiClient )

    val notificationService =
        NotificationService(
            notificationsDao,
            groupsDao,
            fundDao,
            usersDao,
            loansDao,
            eventDispatchService
        )

    val approvalService = ApprovalService(
        approvalDao,
        loansDao,
        groupsDao,
        fundDao,
        usersDao,
        notificationService
    )
    // ------------------ Error Handling ------------------
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
        exception<ValidationException> { call, ex ->
            call.respondError(
                HttpStatusCode.BadRequest,
                ex.errorCode,
                ex.message ?: "Validation failed"
            )
        }
        exception<io.ktor.server.plugins.BadRequestException> { call, cause ->
            call.respondError(HttpStatusCode.BadRequest, "validation", "Bad request", cause.message)
        }
        // JDBI DB errors (SMART MAPPING)
        exception<org.jdbi.v3.core.statement.UnableToExecuteStatementException> { call, cause ->

            val msg = cause.cause?.message ?: ""

            // 🔍 Full logging (VERY IMPORTANT)
            call.application.environment.log.error("DB ERROR FULL", cause)

            when {
                // Duplicate / unique constraint → 409
                msg.contains("duplicate", ignoreCase = true) ||
                        msg.contains("unique", ignoreCase = true) -> {
                    call.respondError(
                        HttpStatusCode.Conflict,
                        "duplicate",
                        "Resource already exists",
                        msg.take(200)
                    )
                }

                // Foreign key → invalid reference → 400
                msg.contains("foreign key", ignoreCase = true) -> {
                    call.respondError(
                        HttpStatusCode.BadRequest,
                        "invalid_reference",
                        "Invalid reference data",
                        msg.take(200)
                    )
                }

                // Null constraint → validation → 400
                msg.contains("null value", ignoreCase = true) -> {
                    call.respondError(
                        HttpStatusCode.BadRequest,
                        "validation",
                        "Missing required field",
                        msg.take(200)
                    )
                }

                // Default DB error → 500
                else -> {
                    call.respondError(
                        HttpStatusCode.InternalServerError,
                        "db_error",
                        "Database error",
                        msg.take(200)
                    )
                }
            }
        }
        exception<java.sql.SQLException> { call, cause ->
            call.respondError(
                HttpStatusCode.InternalServerError,
                "db_error",
                "Database error",
                cause.message?.take(200)
            )
        }


        exception<Throwable> { call, cause ->
            call.respondError(
                HttpStatusCode.InternalServerError,
                "internal",
                "Something went wrong"
            )
        }
    }

    // ------------------ Routes ------------------
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
            passwordHistoryDao,
            eventDispatchService,
            moderatorRegistrationService,
            userManagementService,
            emailVerificationService,
            notificationService,
            approvalDao,
            approvalService,
            aiService
        )
    }
    println(">>> MODULE COMPLETED <<<")
}
