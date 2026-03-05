package com.mynikatech.apnafund.server.common.ratelimit

object RateLimitConfig {
    const val REGISTER_PER_HOUR = 5
    const val RESEND_PER_15_MIN = 3

    const val ONE_HOUR_MS = 60 * 60 * 1000L
    const val FIFTEEN_MIN_MS = 15 * 60 * 1000L
}