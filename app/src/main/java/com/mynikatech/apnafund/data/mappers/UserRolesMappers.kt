package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.UserRoles
import com.mynikatech.apnafund.net.dto.UserRolesDto

fun UserRoles.toDto(): UserRolesDto = UserRolesDto(
    userRoleId = userRoleId,
    userId = userId,
    roleId = roleId,
    status = status

)

// DTO → Room
fun UserRolesDto.toEntity(): UserRoles = UserRoles(
    userRoleId = userRoleId ?: 0,
    userId = userId,
    roleId = roleId,
    status = status
)

fun List<UserRolesDto>.toEntity(): List<UserRoles> = map { d ->
    UserRoles(
        userRoleId = d.userRoleId ?: 0,
        userId = d.userId,
        roleId = d.roleId,
        status = d.status
    )

}

fun List<UserRoles>.toDto(): List<UserRolesDto> = map { d ->
    UserRolesDto(
        userRoleId = d.userRoleId,
        userId = d.userId,
        roleId = d.roleId,
        status = d.status
    )
}