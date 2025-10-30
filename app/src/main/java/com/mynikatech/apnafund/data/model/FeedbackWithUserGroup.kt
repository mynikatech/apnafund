package com.mynikatech.apnafund.data.model

data class FeedbackWithUserGroup(
    val feedbackId: Int,
    val message: String,
    val timestamp: String,
    val userName: String,
    val groupName: String?
)
