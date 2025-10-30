package com.mynikatech.apnafund.ui.user

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.data.model.UserProfile
import com.mynikatech.apnafund.databinding.FragmentUserSummaryBinding
import com.mynikatech.apnafund.session.PreferencesHelper
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.loan.AddLoanDialog
import com.mynikatech.apnafund.ui.loan.LoanAdapter
import com.mynikatech.apnafund.ui.viewmodel.FundSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.LoansViewModel
import com.mynikatech.apnafund.ui.viewmodel.ProfileSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserSummaryViewModel
import com.mynikatech.apnafund.util.Converters
import kotlinx.coroutines.launch

class UserSummaryFragment : Fragment() {

    private lateinit var binding: FragmentUserSummaryBinding

    private val userSummaryViewModel: UserSummaryViewModel by viewModels()

    private val loanViewModel: LoansViewModel by viewModels()

    private val fundSharedViewModel: FundSharedViewModel by activityViewModels()

    private val profileSharedViewModel: ProfileSharedViewModel by activityViewModels()

    private var adapter: LoanAdapter? = null

    private var isFundExpanded = false

    private var totalDepositAmount: Double? = null

    private var totalMaturityAmount: Double? = null

    private var userId = -1

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        binding.textViewWelcomeMessage.text =
            getString(R.string.text_welcome_message, SessionManager.userName)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUserSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            userId = it.getInt("userId")
        }
        Log.d("Sunil", "UserFragment: The user Id is  $userId")
        lifecycleScope.launch {
            val userProfile = userSummaryViewModel.getUserProfile(userId)
            saveUserProfilesSession(userProfile)
            val roleCodes = SessionManager.roleNames
            val bottomNav =
                requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
            val isOnlyMember = roleCodes.contains("MEMBER") && roleCodes.size == 1
            bottomNav.menu.findItem(R.id.adminFragment)?.isVisible = !isOnlyMember
            userSummaryViewModel.userName.observe(viewLifecycleOwner) { nameFromVm ->
                val preferred = SessionManager.getFormattedUserName()
                val display = preferred.ifBlank { nameFromVm }
                binding.textViewWelcomeMessage.text =
                    getString(R.string.text_welcome_message, display)

            }
            findNavController().currentBackStackEntry
                ?.savedStateHandle
                ?.getLiveData<Boolean>("profile_updated")
                ?.observe(viewLifecycleOwner) {
                    if (it == true) {
                        binding.textViewWelcomeMessage.text =
                            getString(R.string.text_welcome_message, SessionManager.getFormattedUserName())
                    }
                }

            val hasGroup = SessionManager.groupId != null
            if (!hasGroup) {
                binding.noGroupMessageContainer.visibility = View.VISIBLE
                binding.GroupMessageContainer.visibility = View.GONE
                if (!(SessionManager.isModerator() || SessionManager.isAdmin()))
                    binding.buttonGoToGroups.visibility = View.GONE
            } else {
                binding.noGroupMessageContainer.visibility = View.GONE
                binding.GroupMessageContainer.visibility = View.VISIBLE
                lazyLoadContent()
            }

            binding.buttonGoToGroups.setOnClickListener {
                //redirect to Amin Tab Group Tab
                val action = UserSummaryFragmentDirections
                    .actionUserSummaryFragmentToAdminFragment("Group")
                findNavController().navigate(action)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun lazyLoadContent() {
        if (adapter == null) {
            adapter = LoanAdapter(
                onEditLoanClick = { loan ->
                    val hasMadeFirstEmi =
                        loan.currTotalIntPaid <= 0.0 // will change it later with approval process
                    showApplyLoanDialog(
                        fundId = loan.fundId,
                        borrowerUserId = userId,
                        borrowerName = SessionManager.userName,
                        existingLoan = loan,
                        allowEditLoan = hasMadeFirstEmi
                    )
                },
                onLoanEmiClick = { loanDetails ->
                    val action = UserSummaryFragmentDirections
                        .actionUserSummaryFragmentToLoanEmiFragment(loanDetails.loanId!!)
                    findNavController().navigate(action)
                }
            )
            binding.recyclerViewLoans.adapter = adapter
        }

        if (userSummaryViewModel.userFunds.value == null) {
            userSummaryViewModel.loadUserSummary(userId)
        }

        userSummaryViewModel.userName.observe(viewLifecycleOwner) {
            binding.textViewWelcomeMessage.text = getString(R.string.text_welcome_message, it)
        }

        userSummaryViewModel.groupName.observe(viewLifecycleOwner) {
            binding.chipGroupName.text = getString(R.string.text_group_user, it)


        }
        userSummaryViewModel.groupId.observe(viewLifecycleOwner) { groupId ->
            binding.chipGroupName.setOnClickListener {
                val action = UserSummaryFragmentDirections
                    .actionUserSummaryFragmentToGroupDetailsFragment(
                        groupId = groupId
                    )
                findNavController().navigate(action)
            }
        }
        userSummaryViewModel.userFunds.observe(viewLifecycleOwner) { funds ->
            val fundNames = funds.map { it.fundName }

            binding.layoutFundSelector.setOnClickListener {
                val fundNamesMap = userSummaryViewModel.userFunds.value?.map { it.fundName }
                    ?: return@setOnClickListener

                AlertDialog.Builder(requireContext())
                    .setTitle("Select Fund")
                    .setItems(fundNamesMap.toTypedArray()) { _, which ->
                        val selectedFund = userSummaryViewModel.userFunds.value!![which]
                        binding.textViewSelectedFund.text = selectedFund.fundName

                        // Update status dot
                        val isInactive =
                            selectedFund.fundStatus == ApnaBankConstants.INACTIVE_STATUS
                        binding.imageFundStatus.setImageResource(
                            if (isInactive) R.drawable.status_inactive_dot else R.drawable.status_active_dot
                        )

                        userSummaryViewModel.loadFundDetails(userId, selectedFund.fundId)
                    }
                    .show()
            }

            if (fundNames.isNotEmpty()) {
                //check if the fundSharedModel has the selectedFund
                val selectedFund = fundSharedViewModel.selectedFund.value
                val fundId: Int
                val fundStatus: String
                val firstFund = funds[0]
                if (selectedFund == null) {
                    fundId = firstFund.fundId
                    fundStatus = firstFund.fundStatus
                    binding.textViewSelectedFund.text = fundNames[0]
                } else {
                    fundId = selectedFund.fundId
                    fundStatus = selectedFund.fundStatus
                    binding.textViewSelectedFund.text = selectedFund.fundName
                }
                userSummaryViewModel.loadFundDetails(userId, fundId)
                val isInactive = fundStatus == ApnaBankConstants.INACTIVE_STATUS
                if (isInactive) {
                    binding.imageFundStatus.setBackgroundResource(R.drawable.status_inactive_dot)
                    binding.imageFundStatus.tooltipText = ApnaBankConstants.INACTIVE_STATUS
                } else {
                    binding.imageFundStatus.setBackgroundResource(R.drawable.status_active_dot)
                    binding.imageFundStatus.tooltipText = ApnaBankConstants.STATUS_ACTIVE
                }
                if (isFundExpanded) {
                    binding.lineFundDetails.visibility =
                        if (isFundExpanded) View.VISIBLE else View.GONE
                    binding.fundDetailsSection.visibility =
                        if (isFundExpanded) View.VISIBLE else View.GONE
                    binding.imageFundExpandCollapse.setImageResource(
                        if (isFundExpanded) R.drawable.ic_up_arrow else R.drawable.ic_down_arrow)
                }

            } else {
                binding.FundMessageContainer.visibility = View.GONE
                binding.noFundsMessageContainer.visibility = View.VISIBLE
            }
        }

        userSummaryViewModel.fundDepositSummary.observe(viewLifecycleOwner) { depositAmount ->
            totalDepositAmount = depositAmount
            updateUserSummary()
        }

        userSummaryViewModel.fundMaturity.observe(viewLifecycleOwner) { maturityAmount ->
            totalMaturityAmount = maturityAmount
            updateUserSummary()
        }
        userSummaryViewModel.selectedFundDetails.observe(viewLifecycleOwner) { fund ->

            if (fund != null) {
                fundSharedViewModel.selectFund(fund)
                binding.textViewFundModeratorValue.text =
                    Converters.formatUserName(fund.moderatorFirstName, fund.moderatorLastName)
                binding.textViewStartDateValue.text = fund.fundStartDate
                binding.textViewFundMaturityDate.text = fund.fundMaturityDate
                val years = fund.fundPeriod / 12
                val months = fund.fundPeriod % 12
                binding.textViewFundPeriodValue.text =
                    Converters.getFormattedPeriodDate(years, months)
                binding.textViewFrequencyValue.text = fund.depositionFrequency
                binding.textViewFundDepAmountValue.text =
                    Converters.formatCurrency(fund.recurringDepositAmount)
                binding.textViewFundTotCurrDepAmountValue.text =
                    Converters.formatCurrency(fund.totalCurrentDeposit)
                binding.textViewFundTotExpDepAmountValue.text =
                    Converters.formatCurrency(fund.totalExpectedDeposit)
                binding.textViewFundTotExpMatAmountValue.text =
                    Converters.formatCurrency(fund.totalExpectedMaturityAmount)

                binding.viewUserDeposits.setOnClickListener {
                    val action = UserSummaryFragmentDirections
                        .actionUserSummaryFragmentToFundDepositsFragment(
                            fundId = fund.fundId,
                            userId = userId,
                            fundName = fund.fundName
                        )
                    findNavController().navigate(action)
                }
                binding.viewFundSummary.setOnClickListener {
                    val action = UserSummaryFragmentDirections
                        .actionUserSummaryFragmentToDetailedFundSummaryFragment(
                            fundId = fund.fundId
                        )
                    findNavController().navigate(action)
                }
                userSummaryViewModel.userLoans.observe(viewLifecycleOwner) { loans ->
                    val hasLoans = !loans.isNullOrEmpty()
                    binding.recyclerViewLoans.visibility = if (hasLoans) View.VISIBLE else View.GONE
                    binding.textViewNoLoans.visibility   = if (hasLoans) View.GONE    else View.VISIBLE
                    binding.textViewLoansHeader.text     = getString(R.string.text_loan)
                    adapter?.setLoans(loans ?: emptyList())
                }
            }
        }

        binding.imageFundExpandCollapse.setOnClickListener {
            isFundExpanded = !isFundExpanded
            binding.lineFundDetails.visibility =
                if (isFundExpanded) View.VISIBLE else View.GONE
            binding.fundDetailsSection.visibility =
                if (isFundExpanded) View.VISIBLE else View.GONE
            binding.imageFundExpandCollapse.setImageResource(
                if (isFundExpanded) R.drawable.ic_up_arrow else R.drawable.ic_down_arrow

            )
        }
        if (Converters.userHasPrivilege(ApnaBankConstants.ADD_APPLY_LOAN_PRIV))
            binding.loanAddFab.visibility = View.VISIBLE
        else
            binding.loanAddFab.visibility = View.GONE
        binding.loanAddFab.setOnClickListener {
            val selectedFundName = binding.textViewSelectedFund.text.toString()
            val fund =
                userSummaryViewModel.userFunds.value?.find { it.fundName == selectedFundName }

            if (fund != null) {
                showApplyLoanDialog(
                    userId,
                    fund.fundId,
                    borrowerName = SessionManager.getFormattedUserName()
                )
            } else {
                Toast.makeText(requireContext(), "Please select a valid fund", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun updateUserSummary() {
        if (totalDepositAmount == null && totalMaturityAmount != null) {
            binding.textViewFundSummaryValue.text = """Total Deposit: ₹0.00  |  Maturity: ${
                Converters.formatCurrency(totalMaturityAmount!!)
            }"""
        } else if (totalDepositAmount != null && totalMaturityAmount != null) {
            binding.textViewFundSummaryValue.text =
                "Total Deposit: ${Converters.formatCurrency(totalDepositAmount!!)} |  Maturity: ${
                    Converters.formatCurrency(totalMaturityAmount!!)
                }"
        } else if (totalDepositAmount != null) {
            binding.textViewFundSummaryValue.text =
                "Total Deposit: ${Converters.formatCurrency(totalDepositAmount!!)} |  Maturity: ₹0.00"
        }
    }

    private fun showApplyLoanDialog(
        borrowerUserId: Int,
        fundId: Int,
        borrowerName: String? = null,
        existingLoan: LoanDetailsWithMemberNames? = null,
        allowEditLoan: Boolean = true
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val fundRateOfInterest = userSummaryViewModel.getfundRateOfInterest(fundId)
            val fund = userSummaryViewModel.getFund(fundId)!!
            //Get current available amount in the Fund display to the user and also add a validation
            val totalAmountAvailable = userSummaryViewModel.getTotalAmountAvailableforFund(fundId)

            val dialog = AddLoanDialog(
                borrowerName = borrowerName,
                totalAmountAvailable = totalAmountAvailable ?: 0.0,
                rateOfInterest = fundRateOfInterest,
                fundMaturityDate = fund.fundMaturityDate,
                existingLoan = existingLoan,
                allowEditLoan = allowEditLoan,
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
                        maturityDate = loanMaturityDate
                    )
                }
            }
            dialog.show(parentFragmentManager, "AddLoanDialog")
        }
    }

    fun saveUserProfilesSession(userProfiles: List<UserProfile>) {
        if (userProfiles.isEmpty()) return

        val userId = userProfiles[0].userId
        val userName = userProfiles[0].userName
        val groupId = userProfiles[0].groupId
        val firstName = userProfiles[0].firstName
        val lastName = userProfiles[0].lastName
        val emailId = userProfiles[0].emailId
        val phoneNumber = userProfiles[0].phoneNumber

        val roleIds = userProfiles.map { it.roleId }.distinct()
        val roleNames = userProfiles.map { it.roleCode }.distinct()
        val prefsHelper = PreferencesHelper(requireContext())
        prefsHelper.saveSession(
            userId = userId,
            userName = userName,
            roleIds = roleIds,
            roleNames = roleNames,
            groupId = groupId,
            token = null, // Actual token if available(todo)
            isPinSet = userProfiles[0].isPinSet ?: false,
            firstName = firstName,
            lastName = lastName ?: "",
            emailId = emailId,
            phoneNumber = phoneNumber
        )
        prefsHelper.loadSession()
    }

    override fun onDestroyView() {
        binding.recyclerViewLoans.adapter = null
        adapter = null
        super.onDestroyView()
    }
}