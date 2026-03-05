package com.mynikatech.apnafund.server.common.ratelimit

object RateLimitKey {

    fun register(ip: String) = "REGISTER_IP:$ip"

    fun resendOtpUser(userId: Int) = "RESEND_USER:$userId"

    fun resendOtpIp(ip: String) = "RESEND_IP:$ip"

    fun loginIp(ip: String) = "LOGIN_IP:$ip"
}