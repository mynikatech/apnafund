package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.api.ApiResponse
import io.ktor.client.statement.*
import io.ktor.client.call.body
import io.ktor.http.isSuccess

class ApiException(
    val code: Int,
    val type: String,
    override val message: String
) : RuntimeException(message)

suspend inline fun <reified T> HttpResponse.unwrap(): T {
    val env: ApiResponse<T> = body()

    if (!env.success) {
        throw ApiException(
            code = env.code,
            type = env.error?.type ?: "UNKNOWN_ERROR",
            message = env.error?.detail ?: env.message ?: "Request failed"
        )
    }

    @Suppress("UNCHECKED_CAST")
    return env.data as T
}

suspend fun HttpResponse.unwrapNoContent() {
    if (!status.isSuccess()) {
        // fallback: try to parse envelope if server still sent one
        throw ApiException(
            code = status.value,
            type = "HTTP_ERROR",
            message = "Request failed"
        )
    }
}

suspend inline fun HttpResponse.unwrapOk(): Boolean {
    val env: ApiResponse<Unit> = this.body()
    return env.success
}
