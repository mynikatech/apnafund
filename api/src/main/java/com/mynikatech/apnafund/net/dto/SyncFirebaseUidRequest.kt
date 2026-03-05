package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyncFirebaseUidRequest(
    val userId: Int,
    val groupId: Int,
    val firebaseUid: String
)
