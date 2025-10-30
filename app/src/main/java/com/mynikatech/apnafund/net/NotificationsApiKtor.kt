package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.UserNotificationsDto
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class NotificationsApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : NotificationsApi {

    private val client get() = clientProvider()

    override suspend fun addNotifications(userNotifications: UserNotificationsDto) {
        client.post("/notifications/add") {
            contentType(ContentType.Application.Json)
            setBody(userNotifications)
        }.body<Unit>()
    }

    override suspend fun updateNotifications(userNotifications: UserNotificationsDto) {
        client.put("/notifications/update") {
            contentType(ContentType.Application.Json)
            setBody(userNotifications)
        }.body<Unit>()
    }

    override suspend fun deleteNotifications(userNotifications: UserNotificationsDto) {
        client.delete("/notifications/delete") {
            contentType(ContentType.Application.Json)
            setBody(userNotifications)
        }.body<Unit>()
    }

    override suspend fun getUserNotifications(userId: Int): List<UserNotificationsDto>? =
        client.get("/notifications/get/user/$userId").unwrap<List<UserNotificationsDto>>()
}
