package com.mynikatech.apnafund.ui.loan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.databinding.ItemFundLoanBinding
import com.mynikatech.apnafund.net.dto.LoanDetailsWithMemberNamesDto
import com.mynikatech.apnafund.util.Converters
import android.graphics.Color

class FundLoanAdapter(
    private val onClick: (LoanDetailsWithMemberNamesDto) -> Unit
) : ListAdapter<LoanDetailsWithMemberNamesDto, FundLoanAdapter.LoanViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanViewHolder {

        val binding = ItemFundLoanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return LoanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LoanViewHolder, position: Int) {

        val loan = getItem(position)

        holder.bind(loan)

        // Alternate row colors
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FAFAFA"))
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE)
        }

        holder.itemView.setOnClickListener {
            onClick(loan)
        }
    }

    class LoanViewHolder(
        private val binding: ItemFundLoanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(loan: LoanDetailsWithMemberNamesDto) {

            val borrower = "${loan.firstName} ${loan.lastName}"

            binding.textBorrower.text = borrower

            binding.textLoanNumber.text =
                "Loan #${loan.loanNumber}"

            binding.textAmount.text =
                "Amount ${Converters.formatCurrency(loan.loanAmount)}"

            binding.textPrincipal.text =
                "Outstanding ${Converters.formatCurrency(loan.currPrincipal)}"

            binding.textInterest.text =
                "Interest ${Converters.formatCurrency(loan.currTotalIntPaid)}"

            binding.textStatus.text =
                "${loan.status} / ${loan.workflowStatus}"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LoanDetailsWithMemberNamesDto>() {

        override fun areItemsTheSame(
            oldItem: LoanDetailsWithMemberNamesDto,
            newItem: LoanDetailsWithMemberNamesDto
        ): Boolean {

            return oldItem.loanId == newItem.loanId
        }

        override fun areContentsTheSame(
            oldItem: LoanDetailsWithMemberNamesDto,
            newItem: LoanDetailsWithMemberNamesDto
        ): Boolean {

            return oldItem == newItem
        }
    }
}