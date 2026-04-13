package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.AIRequest
import com.mynikatech.apnafund.net.dto.AIResponse

interface AIApi {
    suspend fun query(request: AIRequest): AIResponse
}