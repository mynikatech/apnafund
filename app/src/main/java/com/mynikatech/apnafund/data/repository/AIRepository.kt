package com.mynikatech.apnafund.data.repository

import com.mynikatech.apnafund.net.AIApi
import com.mynikatech.apnafund.net.dto.AIRequest
import com.mynikatech.apnafund.net.dto.AIResponse

class AIRepository(
    private val api: AIApi
) {

    suspend fun query(request: AIRequest): AIResponse {
        return api.query(request)
    }
}