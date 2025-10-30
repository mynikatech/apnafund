package com.mynikatech.apnafund.server.api

import com.mynikatech.apnafund.net.api.ApiError
import com.mynikatech.apnafund.net.api.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.response.*

// helper(s) must be visible to a public inline caller:
@PublishedApi internal fun ApplicationCall.traceIdOrHeader(): String =
    request.headers["X-Request-ID"] ?: callIdOrNull() ?: ""

// also make the callId accessor visible to inline
@PublishedApi internal fun ApplicationCall.callIdOrNull(): String? =
    this.attributes.getOrNull(CallId.key)?.toString()

suspend inline fun <reified T> ApplicationCall.respondOk(
    data: T,
    status: HttpStatusCode = HttpStatusCode.OK
) {
    val trace = traceIdOrHeader()
    respond(
        status,
        ApiResponse(
            success = true,
            data = data,
            code = status.value,
            traceId = trace
        )
    )
}

suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    type: String,
    message: String? = null,
    detail: String? = null,
    fields: Map<String, String>? = null
) {
    val trace = traceIdOrHeader()
    respond(
        status,
        ApiResponse<Unit>(
            success = false,
            data = null,
            error = ApiError(type = type, detail = detail ?: message, fields = fields),
            code = status.value,
            message = message,
            traceId = trace
        )
    )
}
