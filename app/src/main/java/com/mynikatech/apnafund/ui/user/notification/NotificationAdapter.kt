package com.mynikatech.apnafund.ui.user.notification

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.data.model.UserNotifications
import com.mynikatech.apnafund.databinding.ItemNotificationBinding
import com.mynikatech.apnafund.util.ApnaBankDate

class NotificationAdapter(
    private val onNotificationClicked: (UserNotifications) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationsViewHolder>() {

    private val notifications = mutableListOf<UserNotifications>()

    fun updateList(newList: List<UserNotifications>) {
        notifications.clear()
        notifications.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemCount() = notifications.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return NotificationsViewHolder(
            ItemNotificationBinding.inflate(inflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: NotificationsViewHolder, position: Int) {
        holder.bind(notifications[position], position)
    }

    inner class NotificationsViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: UserNotifications, position: Int) {
            binding.apply {

                textViewNotificationValue.text = notification.message
                textViewNotificationTime.text =
                    ApnaBankDate.formatServerTimestamp(notification.createdAt)

                viewUnreadDot.visibility =
                    if (!notification.readFlag) View.VISIBLE else View.GONE

                root.setOnClickListener {
                    if (!notification.readFlag) {

                        // Create updated copy
                        val updatedNotification = notification.copy(readFlag = true)

                        // Replace item in list
                        notifications[position] = updatedNotification
                        notifyItemChanged(position)
                    }
                    onNotificationClicked(notification)
                }
            }
        }
    }
}
