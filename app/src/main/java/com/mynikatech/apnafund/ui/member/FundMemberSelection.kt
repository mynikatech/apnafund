package com.mynikatech.apnafund.ui.member

data class FundMemberSelection(
    val userId: Int,
    val displayName: String,
    var isSelected: Boolean = false,
    var isModerator: Boolean = false
)
