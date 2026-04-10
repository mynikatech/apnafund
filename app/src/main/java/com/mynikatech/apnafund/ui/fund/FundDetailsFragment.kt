package com.mynikatech.apnafund.ui.fund

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.FundWithDetails
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.databinding.FragmentFundDetailsBinding
import com.mynikatech.apnafund.net.dto.FundAvailabilityDto
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.loan.AddLoanDialog
import com.mynikatech.apnafund.ui.user.UserFundDepositsFragmentArgs
import com.mynikatech.apnafund.ui.viewmodel.FundViewModel
import com.mynikatech.apnafund.ui.viewmodel.LoansViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserSummaryViewModel
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.FundDetailsTableRowViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FundDetailsFragment : Fragment() {
    private lateinit var binding: FragmentFundDetailsBinding

    private lateinit var tableLayoutFundDetails: TableLayout

    private val userSummaryViewModel: UserSummaryViewModel by viewModels()

    private val fundViewModel: FundViewModel by viewModels()

    private val loanViewModel: LoansViewModel by viewModels()

    private val args: UserFundDepositsFragmentArgs by navArgs()

    override fun onCreateView(

        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentFundDetailsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        /*(requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Fund Detailed Summary"  // Optional
        }*/
        val fundId = args.fundId
        lifecycleScope.launch {
            val fundDetails = userSummaryViewModel.getFundDetails(fundId)
            fetchAllFundDetails(fundDetails)
        }
        val toolbar = view.findViewById<MaterialToolbar>(R.id.fund_details_toolbar)
       // (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
       // (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)

        // Enable back arrow
        val navController = findNavController()
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow) // your back icon
        toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
        toolbar.title = "Fund Detailed Summary"
    }


    private fun fetchAllFundDetails(fund: FundWithDetails) {
        lifecycleScope.launch {
            binding.textViewFundName.text = fund.fundName
            tableLayoutFundDetails = binding.tableFundDetails
            cleanTable(tableLayoutFundDetails)

            // Collect group members for the fund's group
            val fundMembers = fundViewModel.getFundMembersWithNamesForFund(fund.fundId)
            var serialCounter = 1
            fundMembers.forEach { member ->
                // Fetch deposits for this fund + member
                var totalDeposit = 0.0
                var totalLoans = 0.0
                var expectedMatAmount = 0.0
                val userFundDetails =
                    userSummaryViewModel.getUserFundDetails(member.userId, fund.fundId)
                if (userFundDetails != null) {
                    totalDeposit = userFundDetails.totalDeposit
                    totalLoans = userFundDetails.totalLoanAmount
                    expectedMatAmount = userFundDetails.userExpMatAmount
                }
                // total pending amount is curr principal and remaining Int for loan
                val (pendingAmount, intPaid) = withContext(Dispatchers.IO) {
                    val userLoanDetails =
                        userSummaryViewModel.getUserLoanDetails(member.userId, fund.fundId)
                    val pending = userLoanDetails.totalOutstandingAmount
                    val interestPaid = userLoanDetails.totalCurrIntPaid
                    Pair(pending, interestPaid)
                }
                // Build table row
                var applyLoanPriv = false
                if (Converters.userHasPrivilege(ApnaBankConstants.APPROVE_APPLY_LOAN_PRIV))
                    applyLoanPriv = true
                val viewHolder = FundDetailsTableRowViewHolder(requireContext(), applyLoanPriv)
                viewHolder.tvNo.text = serialCounter.toString()
                viewHolder.tvName.setText(
                    getString(
                        R.string.text_member_name,
                        member.firstName,
                        member.lastName
                    )
                )
                viewHolder.tvDepAmt.text = Converters.formatCurrency(totalDeposit)
                viewHolder.tvLoanAmt.text =
                    if (totalLoans > 0) Converters.formatCurrency(totalLoans) else ApnaBankConstants.ZERO_AMOUNT
                viewHolder.tvTotPendingAmt.text =
                    if (pendingAmount > 0) Converters.formatCurrency(pendingAmount) else ApnaBankConstants.ZERO_AMOUNT
                viewHolder.tvTotIntPaid.text =
                    if (intPaid > 0) Converters.formatCurrency(intPaid) else ApnaBankConstants.ZERO_AMOUNT
                viewHolder.tvExpMatAmt.text =
                    if (expectedMatAmount > 0) Converters.formatCurrency(expectedMatAmount) else ApnaBankConstants.ZERO_AMOUNT

                viewHolder.tvApplyLoan.setOnClickListener {
                    showApplyLoanDialog(
                        member.userId,
                        fund.fundId,
                        borrowerName = "${member.firstName} ${member.lastName}"
                    )
                }
                tableLayoutFundDetails.addView(viewHolder.row)
                serialCounter++
            }
        }
    }

    private fun cleanTable(table: TableLayout) {
        val childCount = table.childCount

        // Remove all rows except the first header row
        if (childCount > 1) {
            table.removeViews(1, childCount - 1)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.show()
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showApplyLoanDialog(
        borrowerUserId: Int,
        fundId: Int,
        borrowerName: String,
        existingLoan: LoanDetailsWithMemberNames? = null,
        allowEditLoan: Boolean = true
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val fund = userSummaryViewModel.getFund(fundId)!!
            val fundRateOfInterest = fund.loanInterestRate
            var totalAmounts: FundAvailabilityDto? = null
            //Get current available amount in the Fund display to the user and also add a validation
            try {
                totalAmounts = userSummaryViewModel.getAllAmountAvailableforFund(fundId)
            }catch (e: Exception) {
                Log.e("LoanDialog", "Error fetching fund amounts", e)

                // Optional: show message
                Toast.makeText(
                    requireContext(),
                    "Unable to fetch fund details",
                    Toast.LENGTH_SHORT
                ).show()
            }

            val dialog = AddLoanDialog(
                borrowerName = borrowerName,
                rateOfInterest = fundRateOfInterest,
                fundMaturityDate = fund.fundMaturityDate,
                existingLoan = existingLoan,
                allowEditLoan = allowEditLoan,
                totalAmounts = totalAmounts,
                borrowerUserId = borrowerUserId

            ) { loanAmount, issueDate, period, loanMaturityDate, borrowerUserId ->
                if (existingLoan != null) {
                    existingLoan.loanId?.let {
                        loanViewModel.updateLoan(
                            loanId = it,
                            loanAmount = loanAmount,
                            issueDate = issueDate,
                            period = period.toDouble(),
                            maturityDate = loanMaturityDate,
                            existingloan = existingLoan
                        )
                    }
                } else {
                    userSummaryViewModel.applyLoan(
                        userId = borrowerUserId,
                        fundId = fundId,
                        loanAmount = loanAmount,
                        issueDate = issueDate,
                        period = period.toDouble(),
                        rateOfInt = fundRateOfInterest,
                        maturityDate = loanMaturityDate,
                        autoapprove = true,
                        requestorId = SessionManager.userId
                    )
                }
            }

            dialog.show(parentFragmentManager, "AddLoanDialog")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as? AppCompatActivity)?.setSupportActionBar(null)
    }
}