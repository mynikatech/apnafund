package com.mynikatech.apnafund.ui.fund

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.FundWithDetails
import com.mynikatech.apnafund.databinding.ItemFundBinding
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.FundInputValidator
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FundAdapter(
    private val onFundClick: (FundWithDetails) -> Unit,
    private val onGroupClick: (groupId: Int) -> Unit,
    private val onEditFundClick: (FundWithDetails) -> Unit,
    private val onLoanApply: (fundId: Int) -> Unit,
    private val onViewFundMembersClick: (fundId: Int, groupId: Int) -> Unit
) : RecyclerView.Adapter<FundAdapter.ViewHolder>() {


    private var fundsWithDetails: List<FundWithDetails> = listOf()

    private lateinit var context: Context

    private var isExpanded = false

    override fun getItemCount(): Int = fundsWithDetails.size

    fun setFundsWithDetails(fundsWithDetails: List<FundWithDetails>) {
        this.fundsWithDetails = fundsWithDetails
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        context = parent.context
        return ViewHolder(ItemFundBinding.inflate(inflater, parent, false))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(fundsWithDetails[position])
    }

    inner class ViewHolder(private val binding: ItemFundBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(fundWithDetails: FundWithDetails) {


            binding.apply {

                // --- existing bindings ---
                textViewFundNameValue.text = fundWithDetails.fundName
                // …
                // 1) Parse the maturity date
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val maturityDate = LocalDate.parse(fundWithDetails.fundMaturityDate, formatter)
                val today = LocalDate.now()

                // 2) Determine if it’s closed
                val isClosed = fundWithDetails.fundStatus == ApnaBankConstants.CLOSED_STATUS ||
                        !maturityDate.isAfter(today)  // true if maturity <= today

                // 3) Update status badge
                if (isClosed) {
                    textViewFundStatusValue.text = context.getString(R.string.status_closed)
                    // set a red background & white text
                    textViewFundStatusValue.setBackgroundResource(R.drawable.status_closed_background)
                    textViewFundStatusValue.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.black
                        )
                    )
                } else {
                    textViewFundStatusValue.text = context.getString(R.string.status_active)
                    textViewFundStatusValue.setBackgroundResource(R.drawable.status_active_background)
                    textViewFundStatusValue.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )
                }

                // 4) Enable or disable the edit icon
                imageEditFund.isEnabled = !isClosed
                imageEditFund.alpha = if (isClosed) 0.4f else 1f  // visual hint
                textViewFundNameValue.setOnClickListener {
                    onFundClick(fundWithDetails)
                }
                if (Converters.userHasPrivilege(ApnaBankConstants.APPROVE_APPLY_LOAN_PRIV))
                    imageApplyLoan.visibility = View.VISIBLE
                else
                    imageApplyLoan.visibility = View.GONE
                imageApplyLoan.isEnabled = !isClosed
                imageApplyLoan.setOnClickListener {
                    onLoanApply(fundWithDetails.fundId)
                }
                textViewFundNameValue.text = fundWithDetails.fundName

                textViewFundMemberDetails.setOnClickListener {
                    onViewFundMembersClick(fundWithDetails.fundId, fundWithDetails.groupId)

                }
                // get Fund moderator full name from moderator user Id

                textViewFundModeratorValue.text =
                    "${fundWithDetails.moderatorFirstName} ${fundWithDetails.moderatorLastName}" // to update
                textViewFundStartDateValue.text = fundWithDetails.fundStartDate
                textViewFundMaturityDateValue.text = fundWithDetails.fundMaturityDate
                textViewFundDepFrequencyValue.text = fundWithDetails.depositionFrequency
                val years = fundWithDetails.fundPeriod / 12
                val months = fundWithDetails.fundPeriod % 12
                textViewFundPeriodValue.text = Converters.getFormattedPeriodDate(years, months)
                textViewFundDepAmountValue.text =
                    FundInputValidator.formatCurrency(fundWithDetails.recurringDepositAmount)
                textViewFundTotExpDepAmountValue.text =
                    FundInputValidator.formatCurrency(fundWithDetails.totalExpectedDeposit)
                textViewFundTotCurrDepAmountValue.text =
                    FundInputValidator.formatCurrency(fundWithDetails.totalCurrentDeposit)
                textViewFundTotExpMatAmountValue.text =
                    FundInputValidator.formatCurrency(fundWithDetails.totalExpectedMaturityAmount)
                val groupName = fundWithDetails.groupName
                textViewFundGroupValue.text = groupName
                binding.textViewFundGroupValue.setOnClickListener {
                    onGroupClick(fundWithDetails.groupId)
                }
                if (Converters.userHasPrivilege(ApnaBankConstants.ADD_FUND_PRIV))
                    binding.imageEditFund.visibility = View.VISIBLE
                else
                    binding.imageEditFund.visibility = View.GONE
                binding.imageEditFund.setOnClickListener {
                    if (!isClosed) onEditFundClick(fundWithDetails)
                }
                imageExpandCollapseFund.setOnClickListener {
                    isExpanded = !isExpanded
                    binding.gridFundDepDetails.visibility =
                        if (isExpanded) View.VISIBLE else View.GONE
                    binding.lineFundDepDetails.visibility =
                        if (isExpanded) View.VISIBLE else View.GONE
                    binding.imageExpandCollapseFund.setImageResource(
                        if (isExpanded) R.drawable.ic_up_arrow else R.drawable.ic_down_arrow

                    )
                }
            }
        }
    }
}
