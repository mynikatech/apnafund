package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class TypeDto(
    val typeId: Int? = null,
    val typeCode: String,
    val typeDescription: String,
    val status: String = "ACTIVE"
)