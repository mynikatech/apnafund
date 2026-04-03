package com.mynikatech.apnafund.constants

object ApnaBankConstants {

    const val INACTIVE_STATUS = "InActive"
    const val CLOSED_STATUS = "CLOSED"
    const val ADD_GROUP_PRIV = "ADDGROUP"
    const val ADD_USER_PRIV = "ADDUSER"
    const val ADD_FUND_PRIV = "ADDFUND"
    const val ADD_APPLY_LOAN_PRIV = "APPLYLOAN"
    const val APPROVE_APPLY_LOAN_PRIV = "APPROVELOAN"
    const val ADD_GROUP_MEMBERS_PRIV = "ADDGROUPMEMBER"
    const val ADD_FUND_MEMBERS_PRIV = "ADDFUNDMEMBER"
    const val POSITIVE_BUTTON_TEXT = "Yes"
    const val NEGATIVE_BUTTON_TEXT = "No"
    const val USER_TEXT = "User"
    const val ACTIVATE_TEXT = "Activate"
    const val DEACTIVATE_TEXT = "Deactivate"
    const val ACTIVATED_TEXT = "Activated"
    const val DEACTIVATED_TEXT = "Deactivated"
    const val FIRST_NAME_ERROR_MESSAGE = "First name must be at least 3 characters"
    const val GROUP_NAME_ERROR_MESSAGE = "Group name must be at least 3 characters"
    const val INVALID_EMAIL_ERROR_MESSAGE = "Invalid email format"
    const val INVALID_PHONE_ERROR_MESSAGE = "Enter 10-digit phone (not starting with 0)"
    const val ZERO_AMOUNT = "₹0.0"
    const val ROLE_MEMBER = "MEMBER"
    const val STATUS_PENDING = "PENDING"
    const val STATUS_REJECTED = "REJECTED"
    const val STATUS_ACTIVE = "ACTIVE"
    const val STATUS_INACTIVE = "INACTIVE"
    const val MESSAGE_NOT_PART_OF_ANY_GROUP = "Group: Not part of any Group yet"
    const val TYPING_DEBOUNCE_MS = 2000L
    const val DAILY_MESSAGE_LIMIT = 100
    const val RESEND_COOLDOWN_MS = 2 * 60 * 1000L // 2 minutes


}