package com.mynikatech.apnafund.server.feedback

import com.mynikatech.apnafund.net.dto.FeedbackDto
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.feedbackRoutes(sql: FeedbackSql) = route("/feedback") {
    post {
        val body = call.receive<FeedbackDto>()
        sql.insertFeedback(body)
        call.respond(HttpStatusCode.NoContent)
    }
    get {
        call.respond(sql.getAllFeedbacks()) // [] when empty
    }
    get("with-user-group") {
        call.respond(sql.getAllFeedbacksWithUserGroup()) // [] when empty
    }
}

