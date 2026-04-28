package com.mynikatech.apnafund.ui.admin.user

import android.view.LayoutInflater
import android.view.View
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

            when (item.entityType) {

                "GROUP" -> bindGroup(item)

                "LOAN" -> bindLoan(item)

                "LOAN_CLOSURE" -> bindLoanClosure(item)
            }

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

        private fun bindGroup(item: PendingApprovalDto) {
            binding.layoutGroup.visibility = View.VISIBLE
            binding.textModerator.text =
                "${item.subtitle}: ${item.requesterName}"

            binding.textGroupName.text = item.title
            binding.textGroupDescription.text = item.description ?: "N/A"
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
        items = newItems
        notifyDataSetChanged()
    }
    fun getItems(): List<PendingApprovalDto> = items
}
