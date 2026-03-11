package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class FirebaseSyncRequest(
    val userId: Int,
    val firebaseUid: String,
    val groupIds: List<Int>
)