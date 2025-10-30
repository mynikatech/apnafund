package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.data.model.UserPasswordHistory
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class PasswordHistoryApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : PasswordHistoryApi {

    private val client get() = clientProvider()

    override suspend fun insertPasswordHistory(entry: UserPasswordHistory) {
        client.post("/password-history/add") {
            contentType(ContentType.Application.Json)
            setBody(entry)
        }.unwrap<Int>()
    }

    override suspend fun getLast3PasswordHashes(userId: Int): List<String> =
        client.get("/password-history/get/last3/$userId").unwrap<List<String>>()

    override suspend fun getLastPasswordChangeDate(userId: Int): Long? {
        val res = client.get("/password-history/get/last-change/$userId") {
            expectSuccess = false // don't throw on 404/204
        }
        return when (res.status) {
            HttpStatusCode.NoContent, HttpStatusCode.NotFound -> null
            else -> res.unwrap<Long?>()   // <-- allow nullable in the envelope
        }
    }
}
