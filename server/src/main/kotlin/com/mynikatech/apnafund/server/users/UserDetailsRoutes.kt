package com.mynikatech.apnafund.server.users

import UsersSql
import com.mynikatech.apnafund.net.dto.FundsDto
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.UserDetailsDto
import com.mynikatech.apnafund.net.dto.UserNotificationsDto
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import com.mynikatech.apnafund.server.notifications.NotificationsSql
import com.mynikatech.apnafund.server.userroles.UserRolesSql
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.userDetailsRoute(
    users: UsersSql,
    userRoles: UserRolesSql,
    notifications: NotificationsSql,
) = route("/users") {
    get("get/details/{userId}") {
        val id = call.parameters["userId"]?.toIntOrNull()
        if (id == null) {
            call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId must be an integer"
            ); return@get
        }

        val u: UsersDto = users.getUser(id).firstOrNull()
            ?: return@get call.respondError(HttpStatusCode.NotFound, "not_found", "User not found")

        val roleCodes: List<String> = userRoles.getAllUserRoleCodes(id)
        val notifList: List<UserNotificationsDto> = notifications.getUserNotifications(id)
        val fundsList: List<FundsDto> = users.getFundsForUser(id)
        val group: GroupsDto? = users.getGroupForUser(id).firstOrNull()

        val payload = UserDetailsDto(
            firstName = u.firstName,
            lastName = u.lastName,
            emailId = u.emailId,
            phoneNumber = u.phoneNumber,
            status = u.status,
            userCode = u.userCode,
            userRoles = roleCodes,
            userNotifications = notifList,
            userFunds = fundsList,
            group = group
        )
        call.respondOk(payload)
    }
}
