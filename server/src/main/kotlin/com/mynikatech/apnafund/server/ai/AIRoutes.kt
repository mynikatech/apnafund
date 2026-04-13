package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.api.ValidationException
import com.mynikatech.apnafund.net.dto.AIRequest
import com.mynikatech.apnafund.server.api.respondOk
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.aiRoutes(
    aiService: AIService
) = route("/ai") {

    // POST /ai/query
    post("query") {

        call.safeRoute("AI query failed") {

            val request = call.receive<AIRequest>()

            // 🔹 Basic validation
            if (request.message.isBlank()) {
                throw ValidationException("validation", "message cannot be empty")
            }

            val response = aiService.processQuery(request)

            response
        }
    }
}

inline suspend fun <reified T> ApplicationCall.safeRoute(
    logMessage: String,
    block: suspend () -> T
) {
    try {
        respondOk(block())
    } catch (e: Exception) {

        // ✅ Log only
        application.log.error(logMessage, e)

        // ✅ IMPORTANT: rethrow so StatusPages handles it
        throw e
    }
}