package com.mynikatech.apnafund.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.ui.chat.model.ChatMessage
import com.mynikatech.apnafund.ui.chat.model.ChatRow
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
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
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
            is ChatRow.MessageRow ->
                if (row.message.senderId == currentUid)
                    TYPE_SENT
                else
                    TYPE_RECEIVED
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

            TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_sent, parent, false)
                ChatVH(view)
            }

            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_received, parent, false)
                ChatVH(view)
            }
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

        fun bind(msg: ChatMessage) {

            /* ---- message text ---- */
            itemView.findViewById<TextView>(R.id.textMessage).text = msg.text
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
                itemView.findViewById<LinearLayout>(R.id.reactionLayout)
            reactionLayout.removeAllViews()

            if (msg.reactions.isNotEmpty()) {
                reactionLayout.visibility = View.VISIBLE

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

                        reactionLayout.addView(tv)

                        tv.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start()
                    }
                }
            } else {
                reactionLayout.visibility = View.GONE
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
    }
}
