package com.mynikatech.apnafund.server.common.ratelimit

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RateLimiter(
    private val maxRequests: Int,
    private val windowMillis: Long
) {

    private data class Counter(
        val count: AtomicInteger,
        @Volatile var windowStartMillis: Long
    )

    private val store = ConcurrentHashMap<String, Counter>()

    fun allow(key: String): Boolean {
        val now = System.currentTimeMillis()

        val counter = store.compute(key) { _, existing ->
            if (existing == null || now - existing.windowStartMillis > windowMillis) {
                Counter(AtomicInteger(1), now)
            } else {
                existing.count.incrementAndGet()
                existing
            }
        }!!

        return counter.count.get() <= maxRequests
    }
}
