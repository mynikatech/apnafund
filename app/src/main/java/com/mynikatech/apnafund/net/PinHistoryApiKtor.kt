package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.data.model.UserPinHistory
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class PinHistoryApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : PinHistoryApi {

    private val client get() = clientProvider()

    override suspend fun insertPINHistory(entry: UserPinHistory) {
        client.post("/pin-history/add") {
            contentType(ContentType.Application.Json)
            setBody(entry)
        }.unwrap<Int>()
    }

    override suspend fun getLast3PINHashes(userId: Int): List<String> =
        client.get("/pin-history/get/last3/$userId").unwrap<List<String>>()

    override suspend fun getLastPINChangeDate(userId: Int): Long? =
        client.get("/pin-history/get/last-change/$userId")
            .unwrap<Long>()
}
