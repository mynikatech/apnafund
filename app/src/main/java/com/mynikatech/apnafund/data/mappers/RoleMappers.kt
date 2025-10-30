package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.Privilege
import com.mynikatech.apnafund.data.model.RolePrivilege
import com.mynikatech.apnafund.data.model.Roles
import com.mynikatech.apnafund.data.model.Type
import com.mynikatech.apnafund.net.dto.PrivilegeDto
import com.mynikatech.apnafund.net.dto.RolePrivilegeDto
import com.mynikatech.apnafund.net.dto.RolesDto
import com.mynikatech.apnafund.net.dto.TypeDto

fun RolesDto.toEntity(): Roles = Roles(
    roleId = roleId ?: 0,
    roleCode = roleCode,
    roleDescription = roleDescription,
    status = status
)

fun Roles.toDto(): RolesDto = RolesDto(
    roleId = roleId,
    roleCode = roleCode,
    roleDescription = roleDescription,
    status = status
)

fun List<RolesDto>.toEntity(): List<Roles> = map { it.toEntity() }
fun List<Roles>.toDto(): List<RolesDto> = map { it.toDto() }


fun PrivilegeDto.toEntity(): Privilege = Privilege(
    privilegeId = privilegeId ?: 0,
    privilegeCode = privilegeCode,
    privilegeDescription = privilegeDescription,
    status = status
)

fun Privilege.toDto(): PrivilegeDto = PrivilegeDto(
    privilegeId = privilegeId ?: 0,
    privilegeCode = privilegeCode,
    privilegeDescription = privilegeDescription,
    status = status
)

fun RolePrivilegeDto.toEntity(): RolePrivilege = RolePrivilege(
    rolePrivilegeId = rolePrivilegeId ?: 0,
    privilegeId = privilegeId,
    roleId = roleId,
    status = status
)

fun RolePrivilege.toDto(): RolePrivilegeDto = RolePrivilegeDto(
    rolePrivilegeId = rolePrivilegeId,
    privilegeId = privilegeId,
    roleId = roleId,
    status = status
)


fun TypeDto.toEntity(): Type = Type(
    typeId = typeId ?: 0,
    typeCode = typeCode,
    typeDescription = typeDescription,
    status = status
)

fun Type.toDto(): TypeDto = TypeDto(
    typeId = typeId,
    typeCode = typeCode,
    typeDescription = typeDescription,
    status = status
)






