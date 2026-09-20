package com.mynikatech.apnafund.ui.user

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.data.model.LoanEmiWithMemberNames
import com.mynikatech.apnafund.databinding.ItemLoanEmiBinding
import com.mynikatech.apnafund.util.Converters

class LoanEmiAdapter(private val loanEmis: List<LoanEmiWithMemberNames>) :
    RecyclerView.Adapter<LoanEmiAdapter.LoanEmiViewHolder>() {

    private lateinit var context: Context
    override fun getItemCount() = loanEmis.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanEmiViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        context = parent.context
        return LoanEmiViewHolder(ItemLoanEmiBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: LoanEmiViewHolder, position: Int) {
        val loanEmi = loanEmis[position]
        holder.bind(loanEmi)
    }

    inner class LoanEmiViewHolder(private val binding: ItemLoanEmiBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(loanEmi: LoanEmiWithMemberNames) {
            binding.apply {
                textMonth.text = Converters.formatMonthYear(loanEmi.emiMonth, loanEmi.emiYear)
                textDate.text = loanEmi.emiDepositedDate
                textInterest.text =
                    if ((loanEmi.emiDepositedAmount ?: 0.0) > 0)
                        Converters.formatCurrency(loanEmi.emiDepositedAmount!!)
                    else
                        ""
                textPrepayment.text =
                    if ((loanEmi.prepaymentAmount ?: 0.0) > 0)
                        Converters.formatCurrency(loanEmi.prepaymentAmount!!)
                    else
                        ""
                if ((loanEmi.lateFee ?: 0.0) >= 0)
                    textLateFee.text = Converters.formatCurrency(loanEmi.lateFee ?: 0.0)
                else
                    textLateFee.text = ""

                textTotalPaid.text = Converters.formatCurrency(
                    (loanEmi.emiDepositedAmount ?: 0.0) +
                            (loanEmi.prepaymentAmount ?: 0.0) +
                            (loanEmi.lateFee ?: 0.0)
                )


            }

        }

    }

}
