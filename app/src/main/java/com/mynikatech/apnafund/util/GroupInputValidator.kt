package com.mynikatech.apnafund.util

object GroupInputValidator {

    fun isGroupNameValidInput(name: String?): Boolean {
        return !name.isNullOrBlank() && name.trim().length >= 3
    }

    fun isModeratorSelected(userId: Int): Boolean {
        return userId > 0
    }

    fun isGroupFormValid(name: String?, userId: Int): Boolean {
        return isGroupNameValidInput(name) && isModeratorSelected(userId)
    }
}