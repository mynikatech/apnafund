package com.mynikatech.apnafund.util

object UserInputValidator {

    fun isFirstNameValid(firstName: String?): Boolean {
        return !firstName.isNullOrBlank() && firstName.trim().length >= 3
    }

    fun isEmailValid(email: String?): Boolean {
        return email.isNullOrBlank() || email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
    }

    fun isPhoneValid(phone: String?): Boolean {
        return phone.isNullOrBlank() ||
                phone.matches(Regex("^[1-9][0-9]{9}$"))
    }
}