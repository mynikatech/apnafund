package com.mynikatech.apnafund.net.api

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val code: Int,
    val message: String? = null,
    val traceId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ApiError(
    val type: String,                  // "validation", "not_found", "conflict", "internal", "auth", etc.
    val detail: String? = null,        // safe description
    val fields: Map<String, String>? = null
)