package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.TypeDto

interface TypeApi {

    suspend fun addType(type: TypeDto)

    fun getAllTypes(): List<TypeDto>

    suspend fun updateType(type: TypeDto)

    suspend fun deleteType(type: TypeDto)
}