package com.mynikatech.apnafund.ui.fund

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
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
    private val onViewFundMembersClick: (fundId: Int, groupId: Int) -> Unit,
    private val onCloseFundClick: (FundWithDetails) -> Unit,
    private val onFundLoansClick: (FundWithDetails) -> Unit
) : RecyclerView.Adapter<FundAdapter.ViewHolder>() {


    private var fundsWithDetails: List<FundWithDetails> = listOf()

    private lateinit var context: Context

    private var isExpanded = false

    override fun getItemCount(): Int = fundsWithDetails.size

    fun setFundsWithDetails(newList: List<FundWithDetails>) {
        val diffCallback = FundDiffCallback(this.fundsWithDetails, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.fundsWithDetails = newList
        diffResult.dispatchUpdatesTo(this)
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
                imageMenu.setOnClickListener { view ->

                    val popup = PopupMenu(view.context, view)
                    popup.inflate(R.menu.fund_item_menu)

                    val isActive = !isClosed
                    val canEdit = Converters.userHasPrivilege(ApnaBankConstants.ADD_FUND_PRIV)
                    val canApplyLoan = Converters.userHasPrivilege(
                        ApnaBankConstants.APPROVE_APPLY_LOAN_PRIV
                    )
                    // Edit → only if active + moderator/admin
                    popup.menu.findItem(R.id.menu_edit).isVisible =
                        isActive && canEdit

                    // Close → only if active + moderator/admin
                    popup.menu.findItem(R.id.menu_close).isVisible =
                        isActive && canEdit

                    // Loan → only if active
                    popup.menu.findItem(R.id.menu_loan).isVisible =
                        isActive && canApplyLoan

                    // OR disable instead of hide:
                    // popup.menu.findItem(R.id.menu_loan).isEnabled = isActive

                    popup.setOnMenuItemClickListener {
                        when (it.itemId) {

                            R.id.menu_edit -> {
                                onEditFundClick(fundWithDetails)
                                true
                            }

                            R.id.menu_close -> {
                                onCloseFundClick(fundWithDetails)
                                true
                            }

                            R.id.menu_loan -> {
                                onLoanApply(fundWithDetails.fundId)
                                true
                            }

                            else -> false
                        }
                    }

                    popup.show()
                }

                textViewFundNameValue.setOnClickListener {
                    onFundClick(fundWithDetails)
                }
                textViewFundNameValue.text = fundWithDetails.fundName

                textViewFundMemberDetails.setOnClickListener {
                    onViewFundMembersClick(fundWithDetails.fundId, fundWithDetails.groupId)

                }
                textViewFundLoansSummary.setOnClickListener {
                    onFundLoansClick(fundWithDetails)
                }
                // Disable if no loans to implement TBD
                /*if (fundWithDetails.totalLoans == 0) {
                    textViewFundLoansSummary.alpha = 0.4f
                    textViewFundLoansSummary.isClickable = false
                } else {
                    textViewFundLoansSummary.alpha = 1.0f
                   textViewFundLoansSummary.isClickable = true
                }*/
                // get Fund moderator full name from moderator user Id

                textViewFundModeratorValue.setText(
                    listOfNotNull(
                        fundWithDetails.moderatorFirstName,
                        fundWithDetails.moderatorLastName
                    ).joinToString(" ")
                )
                textDateSummary.setText(
                    context.getString(
                        R.string.fund_date_range,
                        fundWithDetails.fundStartDate,
                        fundWithDetails.fundMaturityDate
                    )
                )
                textViewFundFrequencyValue.text = fundWithDetails.depositionFrequency
                val years = fundWithDetails.fundPeriod / 12
                val months = fundWithDetails.fundPeriod % 12
                textViewFundPeriodValue.text = Converters.getFormattedPeriodDate(years, months)
                textAmountSummary.text = context.getString(
                    R.string.fund_amount_month,
                    FundInputValidator.formatCurrency(fundWithDetails.recurringDepositAmount)
                )
                textViewFundTotalExpectedValue.text =
                    FundInputValidator.formatCurrency(fundWithDetails.totalExpectedDeposit)
                textViewFundTotalCurrentValue.text =
                    FundInputValidator.formatCurrency(fundWithDetails.totalCurrentDeposit)
                textViewFundMaturityAmountValue.text =
                    FundInputValidator.formatCurrency(fundWithDetails.totalExpectedMaturityAmount)
                val groupName = fundWithDetails.groupName
                textViewFundGroupValue.text = groupName
                binding.textViewFundGroupValue.setOnClickListener {
                    onGroupClick(fundWithDetails.groupId)
                }
                imageExpandCollapseFund.setOnClickListener {
                    isExpanded = !isExpanded
                    binding.fundDetailsSection.visibility =
                        if (isExpanded) View.VISIBLE else View.GONE
                    binding.imageExpandCollapseFund.setImageResource(
                        if (isExpanded) R.drawable.ic_up_arrow else R.drawable.ic_down_arrow

                    )
                }
            }
        }
    }

    class FundDiffCallback(
        private val oldList: List<FundWithDetails>,
        private val newList: List<FundWithDetails>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        // Check if items represent the same entity (usually ID)
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].fundId ==
                    newList[newItemPosition].fundId
        }

        // Check if contents are identical
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
