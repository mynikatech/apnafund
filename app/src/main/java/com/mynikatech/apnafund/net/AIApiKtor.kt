package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.AIRequest
import com.mynikatech.apnafund.net.dto.AIResponse
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AIApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : AIApi {

    private val client get() = clientProvider()

    override suspend fun query(request: AIRequest): AIResponse {
        return client.post("/ai/query") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.unwrap<AIResponse>()
    }
}