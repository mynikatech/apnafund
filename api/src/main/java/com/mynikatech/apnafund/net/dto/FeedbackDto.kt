package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackDto(
    val feedbackId: Int? = null,
    val userId: Int,
    val message: String,
    val timestamp: String
)


