package com.mynikatech.apnafund.net

import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import java.io.IOException

suspend inline fun safeApiCall(
    crossinline call: suspend () -> HttpResponse
): HttpResponse {
    try {
        val response = call()

        // 🔥 Centralized status handling
        if (!response.status.isSuccess()) {
            // Try backend envelope first
            try {
                response.unwrap<Unit>()   // will throw ApiException
                error("Unreachable")
            } catch (e: ApiException) {
                throw e
            }
        }

        return response

    } catch (e: ApiException) {
        throw e
    } catch (e: IOException) {
        throw ApiException(
            code = -1,
            type = "NETWORK_ERROR",
            message = "Network error. Check your connection."
        )
    } catch (e: Exception) {
        throw ApiException(
            code = -1,
            type = "UNKNOWN_ERROR",
            message = "Something went wrong"
        )
    }
}

