package com.mynikatech.apnafund.ui.help

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.R

class HelpSectionAdapter(
    private val context: Context,
    private val sections: List<HelpSection>,
    private val onItemClick: (HelpItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    private var displayList: List<HelpListItem> = buildList(sections)

    override fun getItemViewType(position: Int): Int {
        return when (displayList[position]) {
            is HelpListItem.Header -> VIEW_TYPE_HEADER
            is HelpListItem.Item -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_help_section, parent, false)
            HeaderVH(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_help, parent, false)
            ItemVH(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayList[position]) {

            is HelpListItem.Header -> {
                (holder as HeaderVH).title.text =
                    context.getString(item.titleResId)
            }

            is HelpListItem.Item -> {
                val vh = holder as ItemVH
                vh.title.text = context.getString(item.helpItem.titleResId)

                vh.itemView.setOnClickListener {
                    onItemClick(item.helpItem)
                }
            }
        }
    }

    override fun getItemCount() = displayList.size

    // 🔍 Filter (search + sections merged cleanly)
    fun filter(query: String) {
        displayList = if (query.isBlank()) {
            buildList(sections)
        } else {
            val filteredSections = sections.mapNotNull { section ->
                val items = section.items.filter {
                    context.getString(it.titleResId).contains(query, true) ||
                            context.getString(it.descriptionResId).contains(query, true)
                }
                if (items.isNotEmpty()) section.copy(items = items) else null
            }
            buildList(filteredSections)
        }
        notifyDataSetChanged()
    }

    private fun buildList(sections: List<HelpSection>): List<HelpListItem> {
        val list = mutableListOf<HelpListItem>()
        sections.forEach { section ->
            list.add(HelpListItem.Header(section.titleResId))
            section.items.forEach {
                list.add(HelpListItem.Item(it))
            }
        }
        return list
    }

    // ViewHolders
    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textSectionTitle)
    }

    class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textTitle)
    }
}