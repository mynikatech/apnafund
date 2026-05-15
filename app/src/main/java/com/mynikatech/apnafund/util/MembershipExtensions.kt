package com.mynikatech.apnafund.util

import com.mynikatech.apnafund.data.model.FundMembers
import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.net.dto.UserFundMembership
import com.mynikatech.apnafund.net.dto.UserGroupMembership

fun FundMembers.toUserFundMembership(): UserFundMembership {
    return UserFundMembership(
        fundId = fundId,
        role = role,
        status = status
    )
}

fun GroupMembers.toUserGroupMembership(): UserGroupMembership {
    return UserGroupMembership(
        groupId = groupId,
        role = role,
        status = status
    )
}