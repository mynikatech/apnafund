package com.mynikatech.apnafund.ui.help

sealed class HelpListItem {

    data class Header(
        val titleResId: Int
    ) : HelpListItem()

    data class Item(
        val helpItem: HelpItem
    ) : HelpListItem()
}
