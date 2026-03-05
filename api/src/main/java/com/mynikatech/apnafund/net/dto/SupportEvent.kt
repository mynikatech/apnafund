package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class SupportEvent(
    val type: String = "SUPPORT_EMAIL",
    val userId: Long?,
    val userEmail: String?,
    val subject: String,
    val message: String,
    val source: SupportSource,
    val createdAt: Long = System.currentTimeMillis()
)
