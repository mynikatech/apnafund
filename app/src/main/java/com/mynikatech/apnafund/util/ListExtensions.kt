package com.mynikatech.apnafund.util

import com.mynikatech.apnafund.net.dto.AIChatMessage

fun MutableList<AIChatMessage>.replaceThinking(newMessage: AIChatMessage) {
    if (isNotEmpty()) removeAt(size - 1)
    add(newMessage)
}