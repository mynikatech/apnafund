package com.mynikatech.apnafund.ui.assistant

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.net.dto.AIChatMessage
import com.mynikatech.apnafund.net.dto.AIResponseType
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AIChatAdapter :
    ListAdapter<AIChatMessage, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUser) TYPE_USER else TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == TYPE_USER) {
            val view = inflater.inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_chat_ai, parent, false)
            AiViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)

        if (holder is UserViewHolder) {
            holder.text.text = message.message

        } else if (holder is AiViewHolder) {

            holder.text.text = message.message

            val container = holder.itemView.findViewById<LinearLayout>(R.id.table_container)
            container.removeAllViews()
            val jsonData = message.data
            if (jsonData != null) {
                try {
                    when (message.type) {

                        AIResponseType.TABLE -> {
                            container.visibility = View.VISIBLE


                            val data = jsonData.jsonObject
                            val columns = data.get("columns")?.jsonArray
                            val rows = data.get("rows")?.jsonArray

                            // 🔹 Header Row
                            val headerRow = LinearLayout(holder.itemView.context).apply {
                                orientation = LinearLayout.HORIZONTAL
                            }

                            columns?.forEach {
                                val tv = TextView(holder.itemView.context)
                                tv.text = it.jsonPrimitive.content
                                tv.setPadding(12, 8, 12, 8)
                                tv.setTypeface(null, Typeface.BOLD)
                                headerRow.addView(tv)
                            }

                            container.addView(headerRow)

                            // 🔹 Data Rows
                            rows?.forEach { row ->
                                val rowLayout = LinearLayout(holder.itemView.context).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                }

                                row.jsonArray.forEach { cell ->
                                    val tv = TextView(holder.itemView.context)
                                    tv.text = cell.jsonPrimitive.content
                                    tv.setPadding(12, 8, 12, 8)

                                    rowLayout.addView(tv)
                                }

                                container.addView(rowLayout)
                            }
                        }

                        AIResponseType.SUMMARY -> {

                            container.visibility = View.VISIBLE
                            container.removeAllViews()

                            val dataObj = jsonData.jsonObject

                            dataObj.forEach { (key, value) ->

                                val row = LinearLayout(holder.itemView.context).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                }

                                val keyView = TextView(holder.itemView.context).apply {
                                    text = key
                                        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
                                        .replaceFirstChar { it.uppercase() }

                                    setTypeface(null, Typeface.BOLD)
                                    setPadding(8, 6, 8, 6)

                                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                                }

                                val valueView = TextView(holder.itemView.context).apply {
                                    text = value.jsonPrimitive.content
                                    setPadding(8, 6, 8, 6)

                                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                                }

                                row.addView(keyView)
                                row.addView(valueView)

                                container.addView(row)
                            }
                        }

                        else -> {
                            container.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
                    container.visibility = View.GONE
                    e.printStackTrace()
                }

            } else {
                container.visibility = View.GONE
            }
        }
    }

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.text_message)
    }

    class AiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.text_message)
    }

    class DiffCallback : DiffUtil.ItemCallback<AIChatMessage>() {
        override fun areItemsTheSame(oldItem: AIChatMessage, newItem: AIChatMessage): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: AIChatMessage, newItem: AIChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}