package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
class PendingModeratorRequestDto (
        val userId: Int,
        val groupId: Int,
        val moderatorName: String,
        val groupName: String,
        val groupDescription: String?
)
