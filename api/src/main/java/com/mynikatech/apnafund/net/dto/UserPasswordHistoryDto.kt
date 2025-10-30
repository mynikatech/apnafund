package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

/**
 * Epoch millis for changedAt to match your Room model (System.currentTimeMillis()).
 * If the server sets it, you can send null from the app.
 */
@Serializable
data class UserPasswordHistoryDto(
    val id: Int? = null,
    val userId: Int,
    val passwordHash: String,
    val changedAt: Long? = null
)
