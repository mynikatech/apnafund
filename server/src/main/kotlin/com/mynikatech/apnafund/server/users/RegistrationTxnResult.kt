package com.mynikatech.apnafund.server.users

import com.mynikatech.apnafund.net.dto.ModeratorRegistrationResponse

data class RegistrationTxnResult(
    val response: ModeratorRegistrationResponse,
    val groupId: Int
)
