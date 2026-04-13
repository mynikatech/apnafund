package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
enum class AIResponseType {
    TEXT,
    TABLE,
    NAVIGATION,
    SUMMARY,
    ERROR
}
