package com.mynikatech.apnafund.ui.assistant

import android.graphics.Color
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
                            container.removeAllViews()

                            val context = holder.itemView.context
                            val data = jsonData.jsonObject
                            val columns = data["columns"]?.jsonArray
                            val rows = data["rows"]?.jsonArray

                            // 🔹 Function to create cell
                            fun createCell(text: String, isHeader: Boolean = false): TextView {
                                return TextView(context).apply {
                                    this.text = text
                                    setPadding(16, 12, 16, 12)

                                    layoutParams = LinearLayout.LayoutParams(
                                        0,
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        1f // 🔥 Equal width
                                    )

                                    if (isHeader) {
                                        setTypeface(null, Typeface.BOLD)
                                        setBackgroundColor(Color.LTGRAY)
                                    } else {
                                        setBackgroundColor(Color.WHITE)
                                    }

                                    setTextColor(Color.BLACK)
                                }
                            }

                            // 🔹 Header Row
                            val headerRow = LinearLayout(context).apply {
                                orientation = LinearLayout.HORIZONTAL
                            }

                            columns?.forEach {
                                headerRow.addView(createCell(it.jsonPrimitive.content, true))
                            }

                            container.addView(headerRow)

                            // 🔹 Divider
                            fun addDivider() {
                                val divider = View(context).apply {
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        2
                                    ).apply {
                                        setMargins(0, 0, 0, 0)
                                    }
                                    setBackgroundColor(Color.parseColor("#BDBDBD")) // softer gray
                                }
                                container.addView(divider)
                            }

                            addDivider()

                            // 🔹 Data Rows
                            rows?.forEach { row ->
                                val rowLayout = LinearLayout(context).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                }

                                row.jsonArray.forEach { cell ->
                                    rowLayout.addView(createCell(cell.jsonPrimitive.content))
                                }

                                container.addView(rowLayout)
                                addDivider()
                            }
                        }

                        AIResponseType.SUMMARY -> {

                            container.visibility = View.VISIBLE
                            container.removeAllViews()

                            val context = holder.itemView.context
                            val dataObj = jsonData.jsonObject

                            // 🔹 Format key (camelCase → Title Case)
                            fun formatKey(key: String): String {
                                return key
                                    .replace(Regex("([a-z])([A-Z])"), "$1 $2")
                                    .replaceFirstChar { it.uppercase() }
                            }

                            // 🔹 Row builder
                            fun addRow(key: String, value: String) {
                                val row = LinearLayout(context).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                    setPadding(0, 4, 0, 4)
                                }

                                val keyView = TextView(context).apply {
                                    text = formatKey(key)
                                    setTypeface(null, Typeface.BOLD)
                                    setPadding(8, 8, 8, 8)
                                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                                }

                                val valueView = TextView(context).apply {
                                    text = value
                                    setPadding(8, 8, 8, 8)
                                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                                }

                                row.addView(keyView)
                                row.addView(valueView)

                                container.addView(row)
                            }

                            // 🔹 Section title
                            fun addSectionTitle(titleText: String) {
                                val title = TextView(context).apply {
                                    text = formatKey(titleText)
                                    setTypeface(null, Typeface.BOLD)
                                    textSize = 16f
                                    setPadding(8, 16, 8, 8)
                                }
                                container.addView(title)
                            }

                            // 🔹 Divider
                            fun addDivider() {
                                val divider = View(context).apply {
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        2
                                    )
                                    setBackgroundColor(Color.parseColor("#E0E0E0"))
                                }
                                container.addView(divider)
                            }

                            // 🔥 MAIN LOOP
                            dataObj.forEach { (key, value) ->

                                when (value) {

                                    // ✅ Primitive
                                    is JsonPrimitive -> {
                                        addRow(key, value.content)
                                    }

                                    // ✅ Object (e.g., "overall")
                                    is JsonObject -> {
                                        addSectionTitle(key)

                                        value.forEach { (subKey, subValue) ->
                                            if (subValue is JsonPrimitive) {
                                                addRow(subKey, subValue.content)
                                            }
                                        }

                                        addDivider()
                                    }

                                    // ✅ Array (e.g., "groups")
                                    is JsonArray -> {
                                        addSectionTitle(key)

                                        value.forEach { item ->

                                            if (item is JsonObject) {

                                                val card = LinearLayout(context).apply {
                                                    orientation = LinearLayout.VERTICAL
                                                    setPadding(12, 12, 12, 12)

                                                    layoutParams = LinearLayout.LayoutParams(
                                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                                    ).apply {
                                                        setMargins(0, 8, 0, 8)
                                                    }

                                                    setBackgroundColor(Color.parseColor("#F9F9F9"))
                                                }

                                                item.forEach { (subKey, subValue) ->
                                                    if (subValue is JsonPrimitive) {

                                                        val row = LinearLayout(context).apply {
                                                            orientation = LinearLayout.HORIZONTAL
                                                        }

                                                        val keyView = TextView(context).apply {
                                                            text = formatKey(subKey)
                                                            setTypeface(null, Typeface.BOLD)
                                                            layoutParams =
                                                                LinearLayout.LayoutParams(
                                                                    0,
                                                                    WRAP_CONTENT,
                                                                    1f
                                                                )
                                                        }

                                                        val valueView = TextView(context).apply {
                                                            text = subValue.content
                                                            layoutParams =
                                                                LinearLayout.LayoutParams(
                                                                    0,
                                                                    WRAP_CONTENT,
                                                                    1f
                                                                )
                                                        }

                                                        row.addView(keyView)
                                                        row.addView(valueView)

                                                        card.addView(row)
                                                    }
                                                }

                                                container.addView(card)
                                            }
                                        }

                                        addDivider()
                                    }

                                    // Safety fallback
                                    else -> {
                                        // Unknown type → ignore or log
                                    }
                                }
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
            /*message.actions?.forEach { action ->

                val btn = TextView(holder.itemView.context).apply {
                    text = action.label
                    setPadding(16, 8, 16, 8)
                    setBackgroundResource(R.drawable.bg_button)
                    setOnClickListener {
                        // TODO: trigger UIActionType
                    }
                }

                actionsContainer.addView(btn)*/

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