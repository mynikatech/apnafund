package com.mynikatech.apnafund.server.userroles

import com.mynikatech.apnafund.net.dto.BootstrapData
import com.mynikatech.apnafund.net.dto.UserRolesDto
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import com.mynikatech.apnafund.server.roles.RolesSql
import com.mynikatech.apnafund.server.types.TypesSql
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

fun Route.userRolesRoutes(sql: UserRolesSql, roleSql: RolesSql, typeSql: TypesSql) =
    route("/user-roles") {

        // --- GETs ---
        get("get/roles/{userId}") {
            val uid = call.parameters["userId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "userId required"
                )
            call.respondOk(sql.getRolesOfUser(uid)) // List<RolesDto>
        }

        get("/bootstrap/data") {
            call.safeRoute(
                logMessage = "Bootstrap fetch failed",
                clientMessage = "Failed to load app data"
            ) {

                coroutineScope {

                    val rolesDeferred = async { roleSql.listRoles() }
                    val privilegesDeferred = async { roleSql.getAllRolesWithPrivilege() }
                    val typesDeferred = async { typeSql.getAllTypes() }

                    val roles = rolesDeferred.await()
                    val rolePrivilege = privilegesDeferred.await()
                    val types = typesDeferred.await()

                    val rolesMap = roles.mapNotNull { dto ->
                        val id = (dto.roleId as? Number)?.toInt() ?: return@mapNotNull null
                        dto.roleCode to id
                    }.toMap()

                    val rolePrivilegeMap = rolePrivilege
                        .filter { it.roleId != null }
                        .groupBy(
                            keySelector = { it.roleId!! },
                            valueTransform = { it.privilegeCode }
                        )

                    val typesMap = types.associate { it.typeCode to it.typeDescription }

                    val bootstrap = BootstrapData(
                        roles = rolesMap,
                        rolePrivileges = rolePrivilegeMap,
                        types = typesMap
                    )
                    call.respondOk(bootstrap)
                }

            }
        }

        get("get/user-roles/{userId}") {
            val uid = call.parameters["userId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "userId required"
                )
            call.respondOk(sql.getUserRolesOfUser(uid)) // List<UserRolesDto>
        }

        get("get/all/{userId}") {
            val uid = call.parameters["userId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "userId required"
                )
            call.respondOk(sql.getAllUserRoleCodes(uid)) // List<String>
        }

        get("get/all-ids/{userId}") {
            val uid = call.parameters["userId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "userId required"
                )
            call.respondOk(sql.getAllUserRoleIds(uid)) // List<Int>
        }

        get("get/users-for-role/{roleId}") {
            val rid = call.parameters["roleId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "roleId required"
                )
            call.respondOk(sql.getAllUsersForRole(rid)) // List<UserRoles>
        }

        get("get/pending-moderator-requests") {
            call.respondOk(sql.getPendingModeratorRequests()) // List<PendingModeratorRequest>
        }

        // --- POSTs / PUTs / DELETEs ---
        post("add") {
            val body = call.receive<UserRolesDto>()
            val id = sql.addUserRole(body.userId, body.roleId, body.status ?: "PENDING")
            call.respondOk(id, HttpStatusCode.Created)
        }

        post("add/batch") {
            val items = call.receive<List<UserRolesDto>>()           // flat list
            val json = Json {
                explicitNulls = false
                encodeDefaults = true
            }
            val itemsJson = Json {
                explicitNulls = false
                encodeDefaults = true
            }
                .encodeToString(
                    ListSerializer(UserRolesDto.serializer()),
                    items
                )             // -> JSON array
            val count = sql.addUserRoles(itemsJson)
            call.respondOk(count, HttpStatusCode.Created)            // { "data": <count>, ... }
        }

        put("update") {
            val body = call.receive<UserRolesDto>()
            val ok = sql.updateUserRole(body.userRoleId ?: -1, body.status ?: "ACTIVE")
            if (!ok) return@put call.respondError(
                HttpStatusCode.NotFound,
                "not_found",
                "UserRole not found"
            )
            call.respondOk(Unit, HttpStatusCode.NoContent)
        }

        delete("delete/{userRoleId}") {
            val id = call.parameters["userRoleId"]?.toIntOrNull()
                ?: return@delete call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "userRoleId required"
                )
            val ok = sql.removeUserRole(id)
            if (!ok) return@delete call.respondError(
                HttpStatusCode.NotFound,
                "not_found",
                "UserRole not found"
            )
            call.respondOk(Unit, HttpStatusCode.NoContent)
        }

        delete("delete") {
            val body = call.receive<UserRolesDto>()
            val ok = sql.deleteUserRoleByComposite(body.userId, body.roleId)
            if (!ok) return@delete call.respondError(
                HttpStatusCode.NotFound,
                "not_found",
                "UserRole not found"
            )
            call.respondOk(Unit, HttpStatusCode.NoContent)
        }
    }

inline suspend fun <reified T> ApplicationCall.safeRoute(
    logMessage: String,
    clientMessage: String,
    block: suspend () -> T
) {
    try {
        respondOk(block())
    } catch (e: Exception) {

        application.log.error(logMessage, e)

        respondError(
            HttpStatusCode.InternalServerError,
            "internal",
            clientMessage
        )
    }
}

