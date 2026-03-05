package com.mynikatech.apnafund.ui.chat.model

import com.google.firebase.Timestamp

data class ChatMessage(
    var id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null,
    val deliveredTo: List<String> = emptyList(),
    val seenBy: List<String> = emptyList(),
    val reactions: Map<String, List<String>> = emptyMap(),

    // Reply
    val replyToMessageId: String? = null,
    val replySenderName: String? = null,
    val replyPreview: String? = null
)

