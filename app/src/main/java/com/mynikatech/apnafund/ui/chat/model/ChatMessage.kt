package com.mynikatech.apnafund.ui.chat.model

import com.google.firebase.Timestamp

data class ChatMessage(
    var id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String? = null,
    val createdAt: Timestamp? = null,
    val type: MessageType = MessageType.TEXT,
    val deliveredTo: List<String> = emptyList(),
    val seenBy: List<String> = emptyList(),
    val reactions: Map<String, List<String>> = emptyMap(),
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null,

    // Reply
    val replyToMessageId: String? = null,
    val replySenderName: String? = null,
    val replyPreview: String? = null
)

