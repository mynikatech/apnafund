package com.mynikatech.apnafund.server.users

import kotlinx.serialization.Serializable

@Serializable
data class PasswordUpdateRequest(val passwordHash: String)

@Serializable
data class PinUpdateRequest(val pinHash: String)