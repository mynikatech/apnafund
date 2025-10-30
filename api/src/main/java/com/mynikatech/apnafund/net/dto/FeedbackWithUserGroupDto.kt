package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackWithUserGroupDto(
    val feedbackId: Int? = null,
    val message: String,
    val timestamp: String,
    val userName: String,
    val groupName: String?
)