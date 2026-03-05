package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
enum class SupportSource {
    FEEDBACK,
    CONTACT_US,
    SYSTEM,
    ADMIN_ACTION
}