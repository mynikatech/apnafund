package com.mynikatech.apnafund.server.security

import com.mynikatech.apnafund.net.dto.UserPasswordHistoryDto
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.passwordHistoryRoutes(sql: PasswordHistorySql) = route("/password-history") {

    // POST /password-history/add
    post("add") {
        val dto = call.receive<UserPasswordHistoryDto>()
        if (dto.userId == null || dto.passwordHash.isNullOrBlank()) {
            return@post call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId & passwordHash required"
            )
        }
        val id = sql.insertPasswordHistory(dto.userId!!, dto.passwordHash!!)
        call.respondOk(id)
    }

    // GET /password-history/get/last3/{userId}
    get("get/last3/{userId}") {
        val userId = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        val hashes = sql.getLast3PasswordHashes(userId)
        call.respondOk(hashes)
    }

    // GET /password-history/get/last-change/{userId}
    get("get/last-change/{userId}") {
        val userId = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        val ms = sql.getLastPasswordChangeMs(userId)
        call.respondOk(ms)
    }
}
