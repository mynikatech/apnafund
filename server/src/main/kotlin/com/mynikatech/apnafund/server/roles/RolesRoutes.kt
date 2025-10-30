package com.mynikatech.apnafund.server.roles

import com.mynikatech.apnafund.net.dto.PrivilegeDto
import com.mynikatech.apnafund.net.dto.RolePrivilegeDto
import com.mynikatech.apnafund.net.dto.RolesDto
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/** Simple payload for /user-roles/add */
data class AddUserRoleReq(val userId: Int, val roleId: Int, val status: String = "ACTIVE")

fun Route.rolesRoutes(repo: RolesSql) = route("/roles") {

    // ---- Roles ----
    get("get/all") { call.respondOk(repo.listRoles()) }

    get("get/{id}") {
        val id = call.parameters["id"]!!.toInt()
        val r = repo.getRole(id).firstOrNull()
        if (r != null) call.respondOk(r) else call.respondError(
            HttpStatusCode.NotFound,
            "not_found",
            "Role not found"
        )
    }

    post("add") {
        val dto = call.receive<RolesDto>()
        val id = repo.createRole(dto)
        call.respondOk(id, HttpStatusCode.Created)
    }

    put("update/{id}") {
        val id = call.parameters["id"]!!.toInt()
        val dto = call.receive<RolesDto>()
        val ok = repo.updateRole(id, dto)
        if (ok) call.respondOk(Unit, HttpStatusCode.NoContent)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "Role not found")
    }

    delete("delete/{id}") {
        val id = call.parameters["id"]!!.toInt()
        val ok = repo.deleteRole(id)
        if (ok) call.respondOk(Unit, HttpStatusCode.NoContent)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "Role not found")
    }

    get("get/by-code/{code}") {
        val code = call.parameters["code"]!!
        val id = repo.getRoleIdByRoleCode(code)
        if (id != null) call.respondOk(id)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "Role code not found")
    }

    // ---- Role -> Privileges (read-only under /roles/{roleId}/privileges) ----
    route("{roleId}/privileges") {
        get("get/all") {
            val roleId = call.parameters["roleId"]!!.toInt()
            call.respondOk(repo.listRolePrivileges(roleId))
        }
    }
}

fun Route.privilegesRoutes(repo: RolesSql) = route("/privileges") {
    get("get/all") { call.respondOk(repo.listPrivileges()) }

    get("get/{id}") {
        val id = call.parameters["id"]!!.toInt()
        val p = repo.getPrivilege(id).firstOrNull()
        if (p != null) call.respondOk(p) else call.respondError(
            HttpStatusCode.NotFound,
            "not_found",
            "Privilege not found"
        )
    }

    post("add") {
        val dto = call.receive<PrivilegeDto>()
        val id = repo.createPrivilege(dto)
        call.respondOk(id, HttpStatusCode.Created)
    }

    put("update/{id}") {
        val id = call.parameters["id"]!!.toInt()
        val dto = call.receive<PrivilegeDto>()
        val ok = repo.updatePrivilege(id, dto)
        if (ok) call.respondOk(Unit, HttpStatusCode.NoContent)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "Privilege not found")
    }

    delete("delete/{id}") {
        val id = call.parameters["id"]!!.toInt()
        val ok = repo.deletePrivilege(id)
        if (ok) call.respondOk(Unit, HttpStatusCode.NoContent)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "Privilege not found")
    }
}

fun Route.rolePrivilegeRoutes(repo: RolesSql) = route("/role-privilege") {
    // All role->privilege pairs (this is what your client calls)
    get("all") {
        val list = repo.getAllRolesWithPrivilege()
        call.respondOk(list)
    }

    post("add") {
        val dto = call.receive<RolePrivilegeDto>()
        val id = repo.addRolePrivilege(dto)
        call.respondOk(id, HttpStatusCode.Created)
    }

    delete("{rpId}/remove") {
        val rpId = call.parameters["rpId"]!!.toInt()
        val ok = repo.removeRolePrivilege(rpId)
        if (ok) call.respondOk(Unit, HttpStatusCode.NoContent)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "Mapping not found")
    }
}
