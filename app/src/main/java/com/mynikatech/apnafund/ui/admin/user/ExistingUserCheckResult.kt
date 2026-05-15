package com.mynikatech.apnafund.ui.admin.user

import com.mynikatech.apnafund.net.dto.LoginUserResponse

data class ExistingUserCheckResult(
    val state: ExistingUserState,
    val response: LoginUserResponse? = null
)
