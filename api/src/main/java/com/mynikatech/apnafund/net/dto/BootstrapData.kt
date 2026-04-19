package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class BootstrapData(
    val roles: Map<String, Int>,                // roleCode → roleId
    val rolePrivileges: Map<Int, List<String>>, // roleId → privileges
    val types: Map<String, String>              // typeCode → displayName
)
