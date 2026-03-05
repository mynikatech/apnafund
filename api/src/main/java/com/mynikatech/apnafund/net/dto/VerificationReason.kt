package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
enum class VerificationReason {
    REGISTRATION,
    RESEND,
    ADMIN_TRIGGER
}
