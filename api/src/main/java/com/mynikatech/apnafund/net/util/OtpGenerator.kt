package com.mynikatech.apnafund.net.util

object OtpGenerator {
    fun generate(): String =
        (100000..999999).random().toString()
}