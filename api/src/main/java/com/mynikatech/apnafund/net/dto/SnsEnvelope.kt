package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class SnsEnvelope(
    val Type: String,
    val MessageId: String,
    val TopicArn: String,
    val Message: String,
    val Timestamp: String
)
