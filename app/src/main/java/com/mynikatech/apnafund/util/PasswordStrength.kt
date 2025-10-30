package com.mynikatech.apnafund.util

enum class PasswordStrength { WEAK, GOOD, STRONG }

fun assessPasswordStrength(password: String): PasswordStrength {
    val length = password.length
    val hasUpper = password.any { it.isUpperCase() }
    val hasLower = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    return when {
        length >= 8 && hasUpper && hasLower && hasDigit && hasSpecial -> PasswordStrength.STRONG
        length >= 8 && ((hasUpper || hasLower) && hasDigit) -> PasswordStrength.GOOD
        else -> PasswordStrength.WEAK
    }
}