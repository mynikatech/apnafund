package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.HttpClientProvider.client
import com.mynikatech.apnafund.net.dto.FeedbackDto
import com.mynikatech.apnafund.net.dto.FeedbackWithUserGroupDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class FeedbackApiKtor(private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }) :
    FeedbackApi {

    override suspend fun getAllFeedbacks(): List<FeedbackDto> =
        client.get("/users/feedback/all").unwrap<List<FeedbackDto>>()

    override suspend fun insertFeedback(feedback: FeedbackDto): Boolean {
        val response: HttpResponse = client.put("/users/feedback") {
            contentType(ContentType.Application.Json)
            setBody(feedback)
        }
        return response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.OK
    }

    override suspend fun getAllFeedbacksWithUserGroup(): List<FeedbackWithUserGroupDto> {
        val resp: HttpResponse = client.get("/users/feedback/all/usergroup")
        return if (resp.status == HttpStatusCode.NoContent) {
            emptyList()
        } else {
            resp.body()
        }
    }


}