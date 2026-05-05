package com.mynikatech.apnafund.ui.admin.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
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

            binding.buttonApprove.isEnabled = !item.isProcessing
            binding.buttonReject.isEnabled = !item.isProcessing

            binding.buttonApprove.text =
                if (item.isProcessing) "Approving..." else "Approve"

            binding.buttonReject.text =
                if (item.isProcessing) "Rejecting..." else "Reject"

            when (item.entityType) {

                "GROUP" -> bindGroup(item)

                "LOAN" -> bindLoan(item)

                "LOAN_CLOSURE" -> bindLoanClosure(item)
            }

            binding.buttonApprove.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                listener.onApproveClicked(item, bindingAdapterPosition)
            }

            binding.buttonReject.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                listener.onRejectClicked(item, bindingAdapterPosition)
            }
        }

        private fun bindGroup(item: PendingApprovalDto) {
            binding.layoutGroup.visibility = View.VISIBLE
            binding.textModerator.text = item.requesterName
            binding.textGroupName.text = item.groupName
            binding.textGroupDescription.text = item.groupDescription ?: "N/A"
        }

        private fun bindLoan(item: PendingApprovalDto) {
            binding.layoutLoan.visibility = View.VISIBLE

            binding.textLoanBorrower.text = item.requesterName
            binding.textLoanAmount.text = item.loanAmount.toString()
            binding.textLoanPeriod.text = item.loanPeriod
            binding.textLoanInterest.text = item.LoanIntRate
        }

        private fun bindLoanClosure(item: PendingApprovalDto) {
            binding.layoutLoanClosure.visibility = View.VISIBLE

            binding.textClosureBorrower.text = item.requesterName

            binding.textClosureLoanNumber.text =item.loanNumber ?: "-"
            binding.textRequestedAmount.text =  item.requestedAmount
            binding.textOutstanding.text = item.loamOutstandingAmount
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
        val diffCallback = PendingDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }
    fun getItems(): List<PendingApprovalDto> = items


    class PendingDiffCallback(
        private val oldList: List<PendingApprovalDto>,
        private val newList: List<PendingApprovalDto>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].approvalId ==
                    newList[newItemPosition].approvalId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
