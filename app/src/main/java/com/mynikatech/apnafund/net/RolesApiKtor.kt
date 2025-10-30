package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.PrivilegeDto
import com.mynikatech.apnafund.net.dto.RolePrivilegeDto
import com.mynikatech.apnafund.net.dto.RoleWithPrivilegesDto
import com.mynikatech.apnafund.net.dto.RolesDto
import com.mynikatech.apnafund.net.dto.UserRolesDto
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class RolesApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : RolesApi {

    private val client get() = clientProvider()

    // ---- Roles ----
    override suspend fun getAllRoles(): List<RolesDto> =
        client.get("/roles/get/all").unwrap<List<RolesDto>>()


    override suspend fun getRole(id: Int): RolesDto? =
        client.get("/roles/get/$id").unwrap<RolesDto>()

    override suspend fun addRole(dto: RolesDto): Int =
        client.post("/roles/add") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrap<Int>()

    override suspend fun updateRole(id: Int, dto: RolesDto): Boolean =
        client.put("/roles/update/$id") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrapOk()

    override suspend fun deleteRole(id: Int): Boolean =
        client.delete("/roles/delete/$id").unwrapOk()

    override suspend fun getRoleIdByRoleCode(code: String): Int =
        client.get("/roles/get/by-code/$code")
            .unwrap<Int>()

    override suspend fun assignRole(userId: Int, roleId: Int): Boolean {
        val resp = client.post("/user-roles/add") {
            contentType(ContentType.Application.Json)
            setBody(UserRolesDto(userId = userId, roleId = roleId, status = "ACTIVE"))
        }
        return resp.status == HttpStatusCode.Created || resp.status == HttpStatusCode.NoContent
    }

    // ---- Privileges ----
    override suspend fun listPrivileges(): List<PrivilegeDto> =
        client.get("/privileges/get/all").unwrap<List<PrivilegeDto>>()

    override suspend fun getPrivilege(id: Int): PrivilegeDto? =
        client.get("/privileges/get/$id").unwrap<PrivilegeDto>()

    override suspend fun createPrivilege(dto: PrivilegeDto): Int =
        client.post("/privileges/add") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrap<Int>()

    override suspend fun updatePrivilege(id: Int, dto: PrivilegeDto): Boolean =
        client.put("/privileges/update/$id") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrapOk()

    override suspend fun deletePrivilege(id: Int): Boolean =
        client.delete("/privileges/delete/$id").unwrapOk()

    // ---- Role ↔ Privileges ----
    override suspend fun listRolePrivileges(roleId: Int): List<PrivilegeDto> =
        client.get("/roles/$roleId/privileges/get/all").unwrap<List<PrivilegeDto>>()

    override suspend fun addRolePrivilege(dto: RolePrivilegeDto): Int =
        client.post("/role-privilege/add") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrap<Int>()

    override suspend fun removeRolePrivilege(rolePrivilegeId: Int): Boolean =
        client.delete("/role-privilege/$rolePrivilegeId/remove").unwrap<Boolean>()

    override suspend fun getPrivilegesGroupedByRole(): List<RoleWithPrivilegesDto> =
        client.get("/role-privilege/byRole/all").unwrap<List<RoleWithPrivilegesDto>>()

    override suspend fun getAllRolesWithPrivileges(): List<RoleWithPrivilegesDto> =
        client.get("/role-privilege/all").unwrap<List<RoleWithPrivilegesDto>>()
}
