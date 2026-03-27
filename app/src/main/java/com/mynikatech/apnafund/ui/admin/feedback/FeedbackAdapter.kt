package com.mynikatech.apnafund.ui.admin.feedback

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.net.dto.FeedbackWithUserGroupDto

class FeedbackAdapter : ListAdapter<FeedbackWithUserGroupDto, FeedbackAdapter.FeedbackViewHolder>(DiffCallback()) {

    class FeedbackViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(feedback: FeedbackWithUserGroupDto) {
            val groupName = feedback.groupName
            view.findViewById<TextView>(R.id.textUserName).text = view.context.getString(R.string.user_name_label, feedback.userName)
            if(groupName == null)
                view.findViewById<TextView>(R.id.textGroupName).text = ApnaBankConstants.MESSAGE_NOT_PART_OF_ANY_GROUP
            else
                view.findViewById<TextView>(R.id.textGroupName).text =
                    view.context.getString(R.string.group_name_label, feedback.groupName)
            view.findViewById<TextView>(R.id.textMessage).text = feedback.message
            view.findViewById<TextView>(R.id.textTimestamp).text = feedback.timestamp
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feedback, parent, false)
        return FeedbackViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<FeedbackWithUserGroupDto>() {
        override fun areItemsTheSame(old: FeedbackWithUserGroupDto, new: FeedbackWithUserGroupDto) = old.feedbackId == new.feedbackId
        override fun areContentsTheSame(old: FeedbackWithUserGroupDto, new: FeedbackWithUserGroupDto) = old == new
    }
}