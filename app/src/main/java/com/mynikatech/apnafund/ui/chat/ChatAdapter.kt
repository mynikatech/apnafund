package com.mynikatech.apnafund.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.ui.chat.model.ChatMessage
import com.mynikatech.apnafund.ui.chat.model.ChatRow
import com.mynikatech.apnafund.ui.chat.model.MessageType
import com.mynikatech.apnafund.util.toHHmm

class ChatAdapter(
    private val currentUid: String,
    private val onReply: (ChatMessage) -> Unit,
    private val onReact: (ChatMessage, String) -> Unit,
    private val onReplyNavigate: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ChatRow>()

    init {
        setHasStableIds(true)
    }

    companion object {
        private const val TYPE_DATE = 0

        private const val TYPE_SENT_TEXT = 1
        private const val TYPE_RECEIVED_TEXT = 2

        private const val TYPE_SENT_IMAGE = 3
        private const val TYPE_RECEIVED_IMAGE = 4

        private const val TYPE_SENT_DOC = 5
        private const val TYPE_RECEIVED_DOC = 6
    }

    fun submit(list: List<ChatRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return when (val row = items[position]) {
            is ChatRow.MessageRow -> row.message.id.hashCode().toLong()
            is ChatRow.DateHeader -> row.label.hashCode().toLong()
        }
    }

    /* ---------------- VIEW TYPES ---------------- */

    override fun getItemViewType(position: Int): Int {
        return when (val row = items[position]) {

            is ChatRow.DateHeader -> TYPE_DATE

            is ChatRow.MessageRow -> {
                val isSent = row.message.senderId == currentUid

                when (row.message.type) {

                    MessageType.TEXT -> if (isSent) TYPE_SENT_TEXT else TYPE_RECEIVED_TEXT

                    MessageType.IMAGE -> if (isSent) TYPE_SENT_IMAGE else TYPE_RECEIVED_IMAGE

                    MessageType.DOCUMENT -> if (isSent) TYPE_SENT_DOC else TYPE_RECEIVED_DOC

                    else -> if (isSent) TYPE_SENT_TEXT else TYPE_RECEIVED_TEXT
                }
            }
        }
    }

    fun findMessagePosition(messageId: String): Int {
        return items.indexOfFirst {
            it is ChatRow.MessageRow && it.message.id == messageId
        }
    }

    /* ---------------- VIEW HOLDERS ---------------- */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {

            TYPE_DATE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_date, parent, false)
                DateVH(view)
            }

            TYPE_SENT_TEXT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_sent, parent, false)
                ChatVH(view)
            }

            TYPE_RECEIVED_TEXT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_received, parent, false)
                ChatVH(view)
            }

            TYPE_SENT_IMAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_sent_image, parent, false)
                ChatVH(view)
            }

            TYPE_RECEIVED_IMAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_received_image, parent, false)
                ChatVH(view)
            }

            TYPE_SENT_DOC -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_sent_doc, parent, false)
                ChatVH(view)
            }

            TYPE_RECEIVED_DOC -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_received_doc, parent, false)
                ChatVH(view)
            }

            else -> throw IllegalArgumentException("Unknown viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = items[position]) {
            is ChatRow.DateHeader ->
                (holder as DateVH).bind(row)

            is ChatRow.MessageRow ->
                (holder as ChatVH).bind(row.message)
        }
    }

    override fun getItemCount() = items.size

    /* ---------------- DATE VIEW HOLDER ---------------- */

    inner class DateVH(view: View) : RecyclerView.ViewHolder(view) {
        private val textDate: TextView = view.findViewById(R.id.textDate)

        fun bind(row: ChatRow.DateHeader) {
            textDate.text = row.label
        }
    }

    /* ---------------- MESSAGE VIEW HOLDER ---------------- */

    inner class ChatVH(view: View) : RecyclerView.ViewHolder(view) {

        private val textStatus: TextView? =
            view.findViewById(R.id.textStatus)

        private val replyContainer: View =
            view.findViewById(R.id.replyContainer)

        private val replyUser: TextView =
            view.findViewById(R.id.textReplyUser)

        private val replyText: TextView =
            view.findViewById(R.id.textReplyPreview)

        private val imageMessage: ImageView? =
            itemView.findViewById(R.id.imageMessage)

        private val textFileName: TextView? =
            itemView.findViewById(R.id.textFileName)

        private val textFileSize: TextView? =
            itemView.findViewById(R.id.textFileSize)

        fun bind(msg: ChatMessage) {

            /* ---- message text ---- */
            val textMessage = itemView.findViewById<TextView?>(R.id.textMessage)

            if (msg.type == MessageType.TEXT) {
                textMessage?.visibility = View.VISIBLE
                textMessage?.text = msg.text ?: ""
            } else {
                textMessage?.visibility = View.GONE
            }
            itemView.findViewById<TextView>(R.id.textSender)?.text = msg.senderName
            itemView.findViewById<TextView>(R.id.textTime).text =
                msg.createdAt?.toHHmm() ?: ""

            /* ---- delivery status ---- */
            if (msg.senderId == currentUid) {
                textStatus?.text = when {
                    msg.seenBy.isNotEmpty() -> "Seen"
                    msg.deliveredTo.isNotEmpty() -> "Delivered"
                    else -> "Sent"
                }
                textStatus?.visibility = View.VISIBLE
            } else {
                textStatus?.visibility = View.GONE
            }
            if (msg.type == MessageType.IMAGE) {
                imageMessage?.visibility = View.VISIBLE

                // Example using Glide
                imageMessage?.let {
                    Glide.with(itemView)
                        .load(msg.fileUrl)
                        .into(it)
                }
            } else {
                imageMessage?.visibility = View.GONE
            }
            if (msg.type == MessageType.DOCUMENT) {
                textFileName?.visibility = View.VISIBLE
                textFileSize?.visibility = View.VISIBLE
                textFileName?.text = msg.fileName ?: "Document"
                textFileSize?.text = msg.fileSize?.let { formatFileSize(it) } ?: ""
            } else {
                textFileName?.visibility = View.GONE
                textFileSize?.visibility = View.GONE
            }

            /* ---- reply preview ---- */
            if (msg.replyPreview != null) {
                replyContainer.visibility = View.VISIBLE
                replyUser.text = msg.replySenderName
                replyText.text = msg.replyPreview
            } else {
                replyContainer.visibility = View.GONE
            }

            /* ---- reactions ---- */
            val reactionLayout =
                itemView.findViewById<LinearLayout?>(R.id.reactionLayout)

            reactionLayout?.let { layout ->

                layout.removeAllViews()

                if (msg.reactions.isNotEmpty()) {
                    layout.visibility = View.VISIBLE

                    msg.reactions.forEach { (emoji, users) ->
                        if (users.isNotEmpty()) {

                            val tv = TextView(itemView.context).apply {
                                text = "$emoji ${users.size}"
                                textSize = 12f
                                setPadding(8, 4, 8, 4)
                                alpha = 0f
                                scaleX = 0.8f
                                scaleY = 0.8f
                            }

                            layout.addView(tv)

                            // ✅ KEEP THIS — your animation is good
                            tv.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .start()
                        }
                    }

                } else {
                    layout.visibility = View.GONE
                }
            }

            /* ---- double tap ❤️ ---- */
            var lastTapTime = 0L
            itemView.setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastTapTime < 300) {
                    onReact(msg, "❤️")
                }
                lastTapTime = now
            }

            /* ---- long press menu ---- */
            itemView.setOnLongClickListener {
                showMessageActions(msg)
                true
            }

            replyContainer.setOnClickListener {
                msg.replyToMessageId?.let { repliedId ->
                    onReplyNavigate(repliedId)
                }
            }
        }

        private fun showMessageActions(msg: ChatMessage) {
            val popup = PopupMenu(itemView.context, itemView)
            popup.menu.add("Reply")
            popup.menu.add("👍")
            popup.menu.add("❤️")
            popup.menu.add("😂")

            popup.setOnMenuItemClickListener {
                when (it.title.toString()) {
                    "Reply" -> onReply(msg)
                    "👍", "❤️", "😂" -> onReact(msg, it.title.toString())
                }
                true
            }
            popup.show()
        }

        fun formatFileSize(size: Long): String {
            return when {
                size >= 1024 * 1024 -> "${size / (1024 * 1024)} MB"
                size >= 1024 -> "${size / 1024} KB"
                else -> "$size B"
            }
        }
    }
}
