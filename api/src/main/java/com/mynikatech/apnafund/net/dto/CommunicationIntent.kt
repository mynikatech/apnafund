package com.mynikatech.apnafund.net.dto

data class CommunicationIntent(
    val eventType: String,
    val audience: Audience,
    val recipients: List<String>, // userIds or phones resolved later
    val variables: Map<String, String>
)