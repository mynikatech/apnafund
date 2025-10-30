package com.mynikatech.apnafund.data.repository

import com.mynikatech.apnafund.data.dao.TypeDao
import com.mynikatech.apnafund.net.TypeApi

class TypeRepository(
    private val typeDao: TypeDao,
    private val typeApi: TypeApi
) {

    suspend fun getAllTypes(): Map<String, String> {
        return typeApi.getAllTypes()
            .associate { it.typeCode to it.typeDescription }

    }
}