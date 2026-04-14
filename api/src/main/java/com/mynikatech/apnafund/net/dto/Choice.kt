package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class Choice(
    val message: ChatMessageResponse
)
