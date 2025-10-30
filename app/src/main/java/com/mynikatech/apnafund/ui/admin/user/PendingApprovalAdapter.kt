package com.mynikatech.apnafund.ui.admin.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.databinding.ItemPendingApprovalBinding
import com.mynikatech.apnafund.data.model.PendingModeratorRequest

class PendingApprovalAdapter(
    private var items: List<PendingModeratorRequest>,
    private val listener: OnActionClickListener
) : RecyclerView.Adapter<PendingApprovalAdapter.PendingViewHolder>() {

    interface OnActionClickListener {
        fun onApproveClicked(userId: Int, roleId: Int, groupId: Int)
        fun onRejectClicked(userId: Int, roleId: Int, groupId: Int)
    }

    inner class PendingViewHolder(private val binding: ItemPendingApprovalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(request: PendingModeratorRequest) {
            binding.textModeratorName.text = "Moderator: ${request.moderatorName}"
            binding.textGroupName.text = "Group: ${request.groupName}"
            binding.textGroupDescription.text = "Description: ${request.groupDescription ?: "N/A"}"
            val moderatorRoleId = ApnaFundApplication.rolesMap[ApnaBankConstants.ROLE_MODERATOR]
            binding.buttonApprove.setOnClickListener {
                listener.onApproveClicked(request.userId, moderatorRoleId!!,request.groupId)
            }

            binding.buttonReject.setOnClickListener {
                listener.onRejectClicked(request.userId, moderatorRoleId!!, request.groupId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingViewHolder {
        val binding = ItemPendingApprovalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<PendingModeratorRequest>) {
        items = newItems
        notifyDataSetChanged()
    }
}
