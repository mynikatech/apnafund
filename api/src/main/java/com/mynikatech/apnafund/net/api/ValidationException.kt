package com.mynikatech.apnafund.net.api

class ValidationException(
    val errorCode: String,
    message: String? = null
) : RuntimeException(message)