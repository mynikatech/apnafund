package com.mynikatech.apnafund.ui.user.notification

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.data.model.UserNotifications
import com.mynikatech.apnafund.databinding.ItemNotificationBinding

class NotificationAdapter(private val notifications: List<UserNotifications>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationsViewHolder>() {

    private lateinit var context: Context
    override fun getItemCount() = notifications.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        context = parent.context
        return NotificationsViewHolder(ItemNotificationBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: NotificationsViewHolder, position: Int) {
        val notifications = notifications[position]
        holder.bind(notifications)
    }

    inner class NotificationsViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(notifications: UserNotifications) {
            binding.apply {
                textViewNotificationValue.text = notifications.message
            }
        }

    }
}