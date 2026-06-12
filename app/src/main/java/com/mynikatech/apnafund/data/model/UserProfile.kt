package com.mynikatech.apnafund.data.model

import com.mynikatech.apnafund.net.dto.RolesDto
import com.mynikatech.apnafund.net.dto.UserFundMembership
import com.mynikatech.apnafund.net.dto.UserGroup
import com.mynikatech.apnafund.net.dto.UserGroupMembership

data class UserProfile(
    var userId: Int,
    var userName: String,
    var token: String? = null,
    var isLoggedIn: Boolean? = false,
    var isPinSet: Boolean? = false,
    var firstName: String,
    var lastName: String?,
    var emailId: String?,
    var phoneNumber: String,
){
    var roles: List<RolesDto> = emptyList()
    var groups: List<UserGroup> = emptyList()
    var pendingGroups: List<UserGroup> = emptyList()
    var groupMemberships: List<UserGroupMembership> = emptyList()
    var fundMemberships: List<UserFundMembership> = emptyList()
}
