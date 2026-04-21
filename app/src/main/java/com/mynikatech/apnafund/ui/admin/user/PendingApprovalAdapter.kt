package com.mynikatech.apnafund.ui.admin.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.databinding.ItemPendingApprovalBinding
import com.mynikatech.apnafund.net.dto.PendingApprovalDto


class PendingApprovalAdapter(
    private var items: List<PendingApprovalDto>,
    private val listener: OnActionClickListener
) : RecyclerView.Adapter<PendingApprovalAdapter.PendingViewHolder>() {



    interface OnActionClickListener {
        fun onApproveClicked(item: PendingApprovalDto, position: Int)
        fun onRejectClicked(item: PendingApprovalDto, position: Int)
    }

    inner class PendingViewHolder(
        private val binding: ItemPendingApprovalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PendingApprovalDto) {

            binding.textModeratorName.text =
                "${item.subtitle}: ${item.requesterName}"

            binding.textGroupName.text =
                item.title

            binding.textGroupDescription.text =
                item.description ?: "N/A"

            binding.buttonApprove.setOnClickListener {
                binding.buttonApprove.isEnabled = false
                binding.buttonReject.isEnabled = false
                listener.onApproveClicked(item, bindingAdapterPosition)
            }

            binding.buttonReject.setOnClickListener {
                binding.buttonApprove.isEnabled = false
                binding.buttonReject.isEnabled = false
                listener.onRejectClicked(item, bindingAdapterPosition)
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

    fun updateList(newItems: List<PendingApprovalDto>) {
        items = newItems
        notifyDataSetChanged()
    }
    fun getItems(): List<PendingApprovalDto> = items
}
