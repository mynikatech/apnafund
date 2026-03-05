package com.mynikatech.apnafund.server.common.http

import io.ktor.server.application.*

fun ApplicationCall.clientIp(): String {
    // 1️⃣ If behind proxy / ALB / Nginx
    request.headers["X-Forwarded-For"]
        ?.split(",")
        ?.firstOrNull()
        ?.trim()
        ?.let { return it }

    // 2️⃣ Alternative proxy header
    request.headers["X-Real-IP"]
        ?.trim()
        ?.let { return it }

    // 3️⃣ Direct connection (Ktor 2.x safe)
    return request.local.remoteHost
}