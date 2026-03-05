package com.mynikatech.apnafund.net.dto

@kotlinx.serialization.Serializable
data class CloseFundRequest(
    val closedBy: Int,
    val reason: String
)