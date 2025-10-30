package com.mynikatech.apnafund.server.pin

import com.mynikatech.apnafund.net.dto.UserPinHistoryDto
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import com.mynikatech.apnafund.server.security.PinHistorySql
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.pinHistoryRoutes(sql: PinHistorySql) = route("/pin-history") {

    // POST /pin-history/add
    post("add") {
        val dto = call.receive<UserPinHistoryDto>()
        if (dto.userId == null || dto.pinHash.isNullOrBlank()) {
            return@post call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId & pinHash required"
            )
        }
        val id = sql.insertPinHistory(dto.userId!!, dto.pinHash!!)
        call.respondOk(id)
    }

    // GET /pin-history/get/last3/{userId}
    get("get/last3/{userId}") {
        val userId = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        val hashes = sql.getLast3PinHashes(userId)
        call.respondOk(hashes)
    }

    // GET /pin-history/get/last-change/{userId}
    get("get/last-change/{userId}") {
        val userId = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        val ms = sql.getLastPinChangeMs(userId)
        call.respondOk(ms)
    }
}
