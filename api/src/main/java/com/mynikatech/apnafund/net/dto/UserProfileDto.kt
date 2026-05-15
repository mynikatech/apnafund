package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable


@Serializable
data class UserProfileDto(
    var userId: Int,
    var userName: String,
    var token: String? = null,
    var isLoggedIn: Boolean? = false,
    var isPinSet: Boolean? = false,
    var firstName: String,
    var lastName: String? = null,
    var emailId: String,
    var phoneNumber: String

){
    var roles: List<RolesDto> = emptyList()
    var groups: List<UserGroup> = emptyList()
    var pendingGroups: List<UserGroup> = emptyList()
    var groupMemberships: List<UserGroupMembership> = emptyList()
    var fundMemberships: List<UserFundMembership> = emptyList()
}