package com.mynikatech.apnafund.ui.help


data class HelpSection(
    val id: String,
    val titleResId: Int,
    val category: HelpCategory,
    val items: List<HelpItem>
)