package com.mynikatech.apnafund.server.ai

object StatusNormalizer {

    fun normalize(input: String): String {
        return input.trim().lowercase()
    }
}