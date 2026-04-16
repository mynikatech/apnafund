package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
enum class AIAction {
    LIST,
    DETAILS,
    MEMBERS,
    SUMMARY,
    CREATE,
    UPDATE,
    NAVIGATE,
    UNKNOWN
}
