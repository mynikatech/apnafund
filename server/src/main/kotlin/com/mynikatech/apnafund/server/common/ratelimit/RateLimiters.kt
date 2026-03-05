package com.mynikatech.apnafund.server.common.ratelimit

object RateLimiters {

    val registerLimiter = RateLimiter(
        maxRequests = RateLimitConfig.REGISTER_PER_HOUR,
        windowMillis = RateLimitConfig.ONE_HOUR_MS
    )

    val resendOtpLimiter = RateLimiter(
        maxRequests = RateLimitConfig.RESEND_PER_15_MIN,
        windowMillis = RateLimitConfig.FIFTEEN_MIN_MS
    )

    val loginLimiter = RateLimiter(
        maxRequests = 10,
        windowMillis = RateLimitConfig.ONE_HOUR_MS
    )
}