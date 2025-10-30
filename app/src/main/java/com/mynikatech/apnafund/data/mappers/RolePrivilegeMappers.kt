package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.RoleWithPrivileges
import com.mynikatech.apnafund.net.dto.RoleWithPrivilegesDto

fun RoleWithPrivilegesDto.toEntity(): RoleWithPrivileges = RoleWithPrivileges(
    roleCode = roleCode,
    roleId = roleId,
    privilegeCode = privilegeCode
)

fun RoleWithPrivileges.toDto(): RoleWithPrivilegesDto = RoleWithPrivilegesDto(
    roleCode = roleCode,
    roleId = roleId,
    privilegeCode = privilegeCode
)

fun List<RoleWithPrivilegesDto>.toEntity(): List<RoleWithPrivileges> = map { it.toEntity() }
fun List<RoleWithPrivileges>.toDto(): List<RoleWithPrivilegesDto> = map { it.toDto() }
