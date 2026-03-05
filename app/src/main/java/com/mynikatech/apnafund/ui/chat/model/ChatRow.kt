package com.mynikatech.apnafund.ui.chat.model

sealed class ChatRow {
    data class DateHeader(val label: String) : ChatRow()
    data class MessageRow(val message: ChatMessage) : ChatRow()
}