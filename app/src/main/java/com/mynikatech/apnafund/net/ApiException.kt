package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.api.ApiResponse
import io.ktor.client.statement.*
import io.ktor.client.call.body

class ApiException(
    val code: Int,
    val type: String?,
    override val message: String?
) : RuntimeException(message)

suspend inline fun <reified T> HttpResponse.unwrap(): T {
    val env: ApiResponse<T> = this.body()
    if (env.success) return env.data as T
    throw ApiException(env.code, env.error?.type, env.error?.detail ?: env.message)
}

suspend inline fun HttpResponse.unwrapOk(): Boolean {
    val env: ApiResponse<Unit> = this.body()
    return env.success
}
