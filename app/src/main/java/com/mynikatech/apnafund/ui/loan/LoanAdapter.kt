package com.mynikatech.apnafund.ui.loan

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.databinding.ItemLoanBinding
import com.mynikatech.apnafund.util.Converters

class LoanAdapter(
    private val onEditLoanClick: (LoanDetailsWithMemberNames) -> Unit,
    private val onLoanEmiClick: (LoanDetailsWithMemberNames) -> Unit
) :
    RecyclerView.Adapter<LoanAdapter.ViewHolder>() {

    private var loans: List<LoanDetailsWithMemberNames> = listOf()

    private lateinit var context: Context

    private var isExpanded = false

    override fun getItemCount(): Int = loans.size

    fun setLoans(newLoans: List<LoanDetailsWithMemberNames>) {

        val diffCallback = object : DiffUtil.Callback() {

            override fun getOldListSize() = loans.size
            override fun getNewListSize() = newLoans.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return loans[oldItemPosition].loanId ==

                        newLoans[newItemPosition].loanId
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return loans[oldItemPosition] == newLoans[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)

        loans = newLoans
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        context = parent.context
        return ViewHolder(ItemLoanBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(loans[position])
    }


    inner class ViewHolder(private val binding: ItemLoanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(loan: LoanDetailsWithMemberNames) {

            binding.apply {
                textViewLoanNumberValue.text = loan.loanNumber

                textViewLoanAmountValue.text = Converters.formatCurrency(loan.loanAmount)
                val totalMonths = loan.period.toInt()
                val years = totalMonths / 12
                val months = totalMonths % 12
                textViewLoanPeriodValue.text = when {
                    years >= 1 && months > 0 -> "$years yrs $months mos"
                    years > 1 -> "$years yrs"
                    else -> "$months mos"
                }
                textViewLoanIssueDateValue.text = loan.issuedDate
                textViewLoanCurrEmiValue.text = Converters.formatCurrency(loan.emiInterest)
                // calculate emi value original from original principal, period and int
                val origEmi =
                    (loan.loanAmount * loan.rateOfInterest * loan.period / 12) / 100 / loan.period
                textViewLoanOrigEmiValue.text = Converters.formatCurrency(origEmi)
                textViewLoanCurrIntpaidValue.text = Converters.formatCurrency(loan.currTotalIntPaid)
                textViewLoanInterestRateValue.text = buildString {
                    append(loan.rateOfInterest.toString())
                    append("%")
                }
                textViewLoanMaturityDateValue.text = loan.maturityDate
                val isClosed = loan.status == ApnaBankConstants.CLOSED_STATUS
                val isPending = loan.status == ApnaBankConstants.STATUS_PENDING
                if (isClosed) {
                    textViewLoanStatusValue.text = context.getString(R.string.status_closed)
                    textViewLoanStatusValue.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.status_rejected
                        )
                    )
                } else if (isPending) {
                    textViewLoanStatusValue.text = context.getString(R.string.status_pending)
                    textViewLoanStatusValue.setTypeface(null, Typeface.BOLD)
                    textViewLoanStatusValue.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.status_pending
                        )
                    )
                } else {
                    textViewLoanStatusValue.text = context.getString(R.string.status_active)
                    textViewLoanStatusValue.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.status_approved
                        )
                    )
                }
                if (isExpanded) {
                    binding.lineLoanDetails.visibility =
                        if (isExpanded) View.VISIBLE else View.GONE
                    binding.loanDetailsSection.visibility =
                        if (isExpanded) View.VISIBLE else View.GONE
                    binding.imageExpandCollapseLoan.setImageResource(
                        if (isExpanded) R.drawable.ic_up_arrow else R.drawable.ic_down_arrow
                    )
                }
                imageEditLoan.isEnabled = !isClosed
                imageEditLoan.alpha = if (isClosed) 0.4f else 1f  // visual hint
                textViewLoanOutstandingAmountValue.text =
                    Converters.formatCurrency(loan.currPrincipal)
                imageEditLoan.setOnClickListener {
                    if (!isClosed) onEditLoanClick(loan)
                }

                imageExpandCollapseLoan.setOnClickListener {
                    isExpanded = !isExpanded
                    binding.lineLoanDetails.visibility =
                        if (isExpanded) View.VISIBLE else View.GONE
                    binding.loanDetailsSection.visibility =
                        if (isExpanded) View.VISIBLE else View.GONE
                    binding.imageExpandCollapseLoan.setImageResource(
                        if (isExpanded) R.drawable.ic_up_arrow else R.drawable.ic_down_arrow
                    )
                }
                textViewLoanEmiDetails.setOnClickListener {
                    onLoanEmiClick(loan)
                }
            }
        }

    }
}
