package com.mynikatech.apnafund.server.notifications

import com.mynikatech.apnafund.net.dto.UnreadCountDto
import com.mynikatech.apnafund.net.dto.UserNotificationsDto
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import io.ktor.http.*
import io.ktor.server.application.log
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.notificationsRoutes(sql: NotificationsSql) = route("/notifications") {

    // List notifications for user
    get("get/user/{userId}") {
        val userId = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "userId required")
        val rows = sql.getUserNotifications(userId)
        call.respondOk(rows)
    }

    // Add
    post("add") {
        val dto = call.receive<UserNotificationsDto>()
        val id = sql.addUserNotification(dto)
        call.respondOk(id, HttpStatusCode.Created)
    }

    // Update (body contains id)
    put("update") {
        val dto = call.receive<UserNotificationsDto>()
        val ok = sql.updateUserNotification(dto)
        if (!ok) return@put call.respondError(HttpStatusCode.NotFound, "not_found", "Notification not found")
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    // Delete (body contains id)
    delete("delete") {
        val dto = call.receive<UserNotificationsDto>()
        val ok = sql.deleteUserNotification(dto.userNotificationId ?: -1)
        if (!ok) return@delete call.respondError(HttpStatusCode.NotFound, "not_found", "Notification not found")
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    get("/users/{userId}/unread-count") {
        val userId = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )

        try {
            val count = sql.getUnreadNotificationCount(userId)

            call.respondOk(UnreadCountDto(count))

        } catch (e: Exception) {
            call.application.log.error(
                "getUnreadNotificationCount failed for userId=$userId",
                e
            )

            call.respondError(
                HttpStatusCode.InternalServerError,
                "internal",
                "Failed to fetch unread notification count"
            )
        }
    }
    post("/users/{notificationId}/read") {

        val notificationId = call.parameters["notificationId"]?.toIntOrNull()
            ?: return@post call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "notificationId required"
            )

        try {
            val updated = sql.markNotificationRead(notificationId)
            call.application.log.info("markNotificationRead result = $updated")

            if (updated) {
                call.respondOk(
                    mapOf("message" to "Notification marked as read")
                )
            } else {
                call.respondError(
                    HttpStatusCode.NotFound,
                    "not_found",
                    "Notification not found"
                )
            }

        } catch (e: Exception) {
            call.application.log.error(
                "markNotificationRead failed for notificationId=$notificationId",
                e
            )

            call.respondError(
                HttpStatusCode.InternalServerError,
                "internal",
                "Failed to mark notification as read"
            )
        }
    }
}
