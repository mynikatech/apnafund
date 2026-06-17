package com.mynikatech.apnafund.ui.fund

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.FundDetails
import com.mynikatech.apnafund.data.model.FundMembers
import com.mynikatech.apnafund.data.model.FundWithDetails
import com.mynikatech.apnafund.data.model.Funds
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.databinding.DialogAddFundBinding
import com.mynikatech.apnafund.databinding.FragmentFundBinding
import com.mynikatech.apnafund.net.dto.UserDisplay
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.loan.AddLoanDialog
import com.mynikatech.apnafund.ui.viewmodel.FundSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.AdminSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.FundViewModel
import com.mynikatech.apnafund.ui.viewmodel.GroupSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.GroupViewModel
import com.mynikatech.apnafund.ui.viewmodel.LoansViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserSummaryViewModel
import com.mynikatech.apnafund.util.ApnaBankDate
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.FundInputValidator
import com.mynikatech.apnafund.util.showSuccessSnackbar
import com.mynikatech.apnafund.util.toUserFundMembership
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class FundFragment : Fragment() {

    private lateinit var binding: FragmentFundBinding

    private lateinit var adapter: FundAdapter

    private val fundViewModel: FundViewModel by viewModels()

    private val loanViewModel: LoansViewModel by viewModels()

    private val groupViewModel: GroupViewModel by viewModels()

    private val userSummaryViewModel: UserSummaryViewModel by viewModels()

    private val fundSharedViewModel: FundSharedViewModel by activityViewModels()

    private val groupSharedViewModel: GroupSharedViewModel by activityViewModels()

    private val sharedAdminViewModel: AdminSharedViewModel by activityViewModels()

    val isAdmin = SessionManager.isAdmin()
    private val moderatorGroupId = SessionManager.groupId ?: -1

    @RequiresApi(Build.VERSION_CODES.O)
    private val onCloseFundClick: (FundWithDetails) -> Unit = { fund ->
        showCloseFundDialog(fund)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentFundBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        val context = activity

        groupViewModel.fetchCurrentUserGroupMember(moderatorGroupId)
        binding.fundFab.setOnClickListener {
            if (context != null)
                showAddFundDialog(context)
        }

        adapter = FundAdapter(
            onFundClick = { fund ->
                fundSharedViewModel.selectFund(fund)
                val action = FundFragmentDirections
                    .actionFundFragmentToFundDetailsFragment(
                        fundId = fund.fundId
                    )
                findNavController().navigate(action)
            },
            onGroupClick = { groupId ->
                groupSharedViewModel.setSelectedGroupId(groupId)
                val action = FundFragmentDirections
                    .actionFundFragmentToGroupDetailsFragment(
                        groupId = groupId
                    )
                findNavController().navigate(action)
            },
            onEditFundClick = { fund ->
                fundSharedViewModel.selectFund(fund)
                val hasMadeFirstDeposit = fund.totalCurrentDeposit > 0
                showAddFundDialog(requireContext(), existingFund = fund, !hasMadeFirstDeposit)
            },
            onLoanApply = { fundId ->
                showApplyLoanDialog(
                    SessionManager.userId,
                    fundId,
                    borrowerName = SessionManager.getFormattedUserName()
                )
            },
            onViewFundMembersClick = { fundId, groupId ->
                val action = FundFragmentDirections
                    .actionFundFragmentToFundMemberDetailsFragment(
                        fundId = fundId,
                        groupId = groupId
                    )
                findNavController().navigate(action)
            },
            onCloseFundClick = onCloseFundClick,
            onFundLoansClick = { fund ->

                val action =
                    FundFragmentDirections
                        .actionFundFragmentToFundLoanSummaryFragment(
                            fundId = fund.fundId,
                            fundName = fund.fundName
                        )

                findNavController().navigate(action)
            }
        )
        binding.recyclerView.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            fundSharedViewModel.loadFunds(isAdmin, moderatorGroupId)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                fundSharedViewModel.funds.collect {
                    adapter.setFundsWithDetails(it)

                    binding.noFundMessageContainer.visibility =
                        if (it.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupViewModel.currentUserGroupMember.collect { member ->
                    binding.fundFab.visibility =
                        if (Converters.canManageFunds(member)) View.VISIBLE else View.GONE
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showCloseFundDialog(fund: FundWithDetails) {

        val input = EditText(requireContext()).apply {
            hint = "Reason for closure"
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.text_close_fund))
            .setMessage(getString(R.string.message_no_further_deposits_loans_allowed))
            .setView(input)
            .setNegativeButton(getString(R.string.text_cancel_button), null)
            .setPositiveButton(getString(R.string.text_close_fund)) { _, _ ->

                val reason =
                    input.text.toString().ifBlank {
                        "Fund closed by moderator"
                    }

                closeFund(fund, reason)
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun closeFund(
        fund: FundWithDetails,
        reason: String
    ) {

        lifecycleScope.launch {

            try {

                fundViewModel.closeFund(
                    fundId = fund.fundId,
                    reason = reason,
                    userId = SessionManager.userId
                )
                fetchAllFunds(adapter)

                Toast.makeText(
                    requireContext(),
                    getString(R.string.message_fund_closed_success),
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Log.e("FundFragment", e.message.toString())
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_unable_close_fund),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddFundDialog(
        context: Context, existingFund: FundWithDetails? = null,
        allowEditFinancials: Boolean = true
    ) {

        val dialogBinding = DialogAddFundBinding.inflate(LayoutInflater.from(context))
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(dialogBinding.root)
        dialog.show()
        var groupId = -1
        var moderator = -1
        val needsTabRefresh = !SessionManager.canManageDeposits()
        fun updateCreatorMembershipUiState(
            moderatorId: Int
        ) {

            val isSelfModerator =
                moderatorId == SessionManager.userId

            if (isSelfModerator) {

                dialogBinding.checkBoxExcludeCreator.apply {
                    isChecked = false
                    isEnabled = false
                }

            } else {

                dialogBinding.checkBoxExcludeCreator.isEnabled = true
            }
        }
        dialogBinding.buttonSaveFund.isEnabled = false
        dialogBinding.checkBoxVariableInterestRate.setOnCheckedChangeListener { _, checked ->

            dialogBinding.layoutInterestRevision.visibility =
                if (checked) View.VISIBLE else View.GONE

            if (!checked) {

                dialogBinding.editTextRevisedLoanInterestRate.text?.clear()

                dialogBinding.editTextInterestRateRevisionMonths.text?.clear()
            }

            updateSaveButtonState(
                dialogBinding,
                groupId,
                moderator
            )
        }
        dialogBinding.setupForm()
        if (existingFund != null) {
            dialogBinding.buttonSaveFund.text = getString(R.string.text_update_fund)
            dialogBinding.editTextFundName.setText(existingFund.fundName)
            dialogBinding.editTextFundStartDate.setText(existingFund.fundStartDate)
            dialogBinding.editTextPeriod.setText(existingFund.fundPeriod.toString())
            dialogBinding.editTextDepFrequency.setText(existingFund.depositionFrequency)
            dialogBinding.editTextDepAmount.setText(existingFund.recurringDepositAmount.toString())
            dialogBinding.editTextLoanIntRate.setText(existingFund.loanInterestRate.toString())
            dialogBinding.editTextLateFeeRate.setText(existingFund.lateFeeRate.toString())
            dialogBinding.editTextDepositLastDate.setText(existingFund.monthlyDepDateBy.toString())
            dialogBinding.checkBoxExcludeCreator.visibility = View.GONE
            dialogBinding.checkBoxVariableInterestRate.isChecked =
                existingFund.hasVariableInterestRate
            dialogBinding.editTextRevisedLoanInterestRate.setText(
                existingFund.revisedLoanInterestRate?.toString() ?: ""
            )
            dialogBinding.editTextInterestRateRevisionMonths.setText(
                existingFund.interestRateRevisionAfterMonths?.toString() ?: ""
            )
            dialogBinding.layoutInterestRevision.visibility =
                if (existingFund.hasVariableInterestRate)
                    View.VISIBLE
                else
                    View.GONE
            moderator = existingFund.moderator
            groupId = existingFund.groupId

            dialogBinding.apply {
                editTextFundStartDate.isEnabled = allowEditFinancials
                editTextPeriod.isEnabled = allowEditFinancials
                editTextDepAmount.isEnabled = allowEditFinancials
                editTextLoanIntRate.isEnabled = allowEditFinancials
                editTextLateFeeRate.isEnabled = allowEditFinancials
                editTextDepositLastDate.isEnabled = allowEditFinancials
                editTextDepFrequency.isEnabled = allowEditFinancials
                //gray them out
                val alpha = if (allowEditFinancials) 1f else 0.5f
                editTextFundStartDate.alpha = alpha
                editTextPeriod.alpha = alpha
                editTextDepAmount.alpha = alpha
                editTextLoanIntRate.alpha = alpha
                editTextLateFeeRate.alpha = alpha
                editTextDepositLastDate.alpha = alpha
                editTextDepFrequency.alpha = alpha
            }
        }
        lifecycleScope.launch {
            val groupsFlow =
                groupViewModel.fetchAllGroups()
            groupsFlow.collectLatest { groups ->
                val groupNames = groups.map { it.groupName }
                val idToIndex = groups.mapIndexed { idx, g -> g.groupId to idx }.toMap()
                val adapter = ArrayAdapter(
                    context,
                    R.layout.dropdown_item_apnabank,
                    groupNames
                )
                dialogBinding.editTextAutoGroup.setAdapter(adapter)

                if (existingFund != null) {
                    idToIndex[existingFund.groupId]?.let { idx ->
                        dialogBinding.editTextAutoGroup.setText(groupNames[idx], false)
                        groupId = existingFund.groupId
                        dialogBinding.buttonSaveFund.isEnabled =
                            validateInputs(dialogBinding, groupId, moderator)
                    }
                    dialogBinding.editTextAutoGroup.isEnabled = false
                    refreshModeratorDropdown(
                        groupId,
                        dialogBinding,
                        context,
                        existingFund,
                        defaultModerator = moderator
                    ) { members, preselectedId->

                        // Now setup dropdown WITH data
                        setupModeratorDropdown(dialogBinding, members) { selectedId ->
                            moderator = selectedId
                            updateCreatorMembershipUiState(
                                moderator
                            )
                            dialogBinding.buttonSaveFund.isEnabled =
                                validateInputs(dialogBinding, groupId, moderator)
                        }
                        // 🔥 set default moderator properly
                        if (preselectedId != null) {
                            moderator = preselectedId
                            updateCreatorMembershipUiState(moderator)
                            updateSaveButtonState(dialogBinding, groupId, moderator)
                        }
                    }
                } else if (!isAdmin) {
                    idToIndex[moderatorGroupId]?.let { idx ->
                        dialogBinding.editTextAutoGroup.setText(groupNames[idx], false)
                        groupId = moderatorGroupId
                        dialogBinding.buttonSaveFund.isEnabled =
                            validateInputs(dialogBinding, groupId, moderator)
                        refreshModeratorDropdown(
                            groupId,
                            dialogBinding,
                            context,
                            existingFund,
                            defaultModerator = moderator
                        )  { members, preselectedId->

                            // Now setup dropdown WITH data
                            setupModeratorDropdown(dialogBinding, members) { selectedId ->
                                moderator = selectedId
                                dialogBinding.buttonSaveFund.isEnabled =
                                    validateInputs(dialogBinding, groupId, moderator)
                            }
                            // set default moderator properly
                            if (preselectedId != null) {
                                moderator = preselectedId
                                updateCreatorMembershipUiState(moderator)
                                updateSaveButtonState(dialogBinding, groupId, moderator)
                            }
                        }
                    }
                    dialogBinding.editTextAutoGroup.isEnabled = false
                }
                dialogBinding.editTextAutoGroup.threshold = 0

                dialogBinding.editTextAutoGroup.setOnClickListener {
                    dialogBinding.editTextAutoGroup.showDropDown()
                }
                dialogBinding.editTextAutoGroup.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        dialogBinding.editTextAutoGroup.post { dialogBinding.editTextAutoGroup.showDropDown() }
                    }
                }
                dialogBinding.editTextAutoGroup.setOnItemClickListener { _, _, position, _ ->
                    groupId = groups[position].groupId
                    dialogBinding.buttonSaveFund.isEnabled =
                        validateInputs(dialogBinding, groupId, moderator)
                    refreshModeratorDropdown(
                        groupId,
                        dialogBinding,
                        context,
                        existingFund,
                        defaultModerator = moderator
                    ) { members, preselectedId->

                        // Now setup dropdown WITH data
                        setupModeratorDropdown(dialogBinding, members) { selectedId ->
                            moderator = selectedId
                            dialogBinding.buttonSaveFund.isEnabled =
                                validateInputs(dialogBinding, groupId, moderator)
                        }
                        // set default moderator properly
                        if (preselectedId != null) {
                            moderator = preselectedId
                            updateCreatorMembershipUiState(moderator)
                            updateSaveButtonState(dialogBinding, groupId, moderator)
                        }
                    }
                }
                dialogBinding.editTextAutoGroup.addTextChangedListener {
                    if (it.isNullOrBlank()) {
                        groupId = -1
                        dialogBinding.buttonSaveFund.isEnabled =
                            validateInputs(dialogBinding, groupId, moderator)
                    }
                }
                dialogBinding.editTextFundStartDate.isFocusable = false
                dialogBinding.editTextFundStartDate.setOnClickListener {
                    showDatePickerDialog(dialogBinding.editTextFundStartDate)
                }
            }
        }
        // add TextChangeListener
        dialogBinding.editTextFundName.addTextChangedListener {
            updateSaveButtonState(dialogBinding, groupId, moderator)
        }
        dialogBinding.editTextFundStartDate.addTextChangedListener {
            updateSaveButtonState(dialogBinding, groupId, moderator)
        }
        dialogBinding.editTextPeriod.addTextChangedListener {
            updateSaveButtonState(dialogBinding, groupId, moderator)
        }
        dialogBinding.editTextDepFrequency.addTextChangedListener {
            updateSaveButtonState(dialogBinding, groupId, moderator)
        }
        dialogBinding.editTextDepAmount.doAfterTextChanged {
            updateSaveButtonState(dialogBinding, groupId, moderator)
        }
        dialogBinding.editTextLoanIntRate.addTextChangedListener {
            updateSaveButtonState(dialogBinding, groupId, moderator)
        }
        dialogBinding.editTextRevisedLoanInterestRate.addTextChangedListener {
            updateSaveButtonState(dialogBinding, groupId, moderator)
        }

        dialogBinding.editTextInterestRateRevisionMonths.addTextChangedListener {
            updateSaveButtonState(dialogBinding, groupId, moderator)
        }
        dialogBinding.editTextLateFeeRate.addTextChangedListener {
            updateSaveButtonState(dialogBinding, groupId, moderator)
        }
        dialogBinding.editTextDepositLastDate.addTextChangedListener {
            updateSaveButtonState(dialogBinding, groupId, moderator)
        }
        dialogBinding.imageInfoExcludeCreator.setOnClickListener {

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.title_exclude_creator_info))
                .setMessage(
                    getString(R.string.info_exclude_creator)
                )
                .setPositiveButton(
                    getString(R.string.text_button_ok),
                    null
                )
                .show()
        }

        dialogBinding.buttonSaveFund.setOnClickListener {
            dialogBinding.buttonSaveFund.isEnabled = false
            dialogBinding.buttonSaveFund.text = getString(R.string.button_saving_progress)
            lifecycleScope.launch {
                try {
                    // add Save Fund code
                    // Calculate the Maturity Date from Period
                    val maturityDate = ApnaBankDate.calculateMatDate(
                        dialogBinding.editTextFundStartDate.text.toString().trim(),
                        dialogBinding.editTextPeriod.text.toString().trim().toDouble()
                    )
                    val excludeCreator =
                        dialogBinding.checkBoxExcludeCreator.isChecked
                    if (
                        moderator == SessionManager.userId &&
                        excludeCreator
                    ) {

                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_moderator_must_be_member),
                            Toast.LENGTH_LONG
                        ).show()

                        dialogBinding.buttonSaveFund.isEnabled = true
                        dialogBinding.buttonSaveFund.text =
                            getString(R.string.text_save_button)

                        return@launch
                    }
                    var recalculateFinance = false
                    // check if fund period or deposit amount has been updated, if so recalculate everything
                    val fundPeriod = dialogBinding.editTextPeriod.text.toString().trim().toDouble()
                    val fundName = dialogBinding.editTextFundName.text.toString().trim()
                    val recurringDepositAmount =
                        dialogBinding.editTextDepAmount.text.toString().trim()
                            .toDouble()

                    val loanInterestRate =
                        dialogBinding.editTextLoanIntRate.text.toString().trim().toDouble()

                    val hasVariableInterestRate =
                        dialogBinding.checkBoxVariableInterestRate.isChecked

                    val revisedLoanInterestRate =
                        if (hasVariableInterestRate)
                            dialogBinding.editTextRevisedLoanInterestRate.text.toString().trim().toDouble()
                        else null

                    val interestRateRevisionAfterMonths =
                        if (hasVariableInterestRate)
                            dialogBinding.editTextInterestRateRevisionMonths.text.toString().trim().toInt()
                        else null
                    if (
                        existingFund?.fundPeriod != fundPeriod ||
                        existingFund.recurringDepositAmount != recurringDepositAmount ||
                        existingFund.loanInterestRate != loanInterestRate ||
                        existingFund.hasVariableInterestRate != hasVariableInterestRate ||
                        existingFund.revisedLoanInterestRate != revisedLoanInterestRate ||
                        existingFund.interestRateRevisionAfterMonths != interestRateRevisionAfterMonths
                    ) {
                        recalculateFinance = true
                    }
                    val fundToSave = Funds(
                        fundId = existingFund?.fundId ?: 0,
                        fundName = fundName,
                        fundStartDate = dialogBinding.editTextFundStartDate.text.toString().trim(),
                        fundMaturityDate = maturityDate.trim(),
                        fundPeriod = dialogBinding.editTextPeriod.text.toString().trim().toDouble(),
                        depositionFrequency = dialogBinding.editTextDepFrequency.text.toString()
                            .trim(),
                        recurringDepositAmount = dialogBinding.editTextDepAmount.text.toString()
                            .trim()
                            .toDouble(),
                        loanInterestRate = dialogBinding.editTextLoanIntRate.text.toString().trim()
                            .toDouble(),
                        hasVariableInterestRate =
                            dialogBinding.checkBoxVariableInterestRate.isChecked,

                        revisedLoanInterestRate =
                            if (dialogBinding.checkBoxVariableInterestRate.isChecked)
                                dialogBinding.editTextRevisedLoanInterestRate.text
                                    .toString()
                                    .trim()
                                    .toDouble()
                            else
                                null,

                        interestRateRevisionAfterMonths =
                            if (dialogBinding.checkBoxVariableInterestRate.isChecked)
                                dialogBinding.editTextInterestRateRevisionMonths.text
                                    .toString()
                                    .trim()
                                    .toInt()
                            else
                                null,
                        lateFeeRate = dialogBinding.editTextLateFeeRate.text.toString().trim()
                            .toDouble(),
                        monthlyDepDateBy = dialogBinding.editTextDepositLastDate.text.toString()
                            .toInt(),
                        groupId = groupId,
                        moderator = moderator,
                        fundStatus = existingFund?.fundStatus ?: "ACTIVE",
                        fundCode = Converters.generateFundCode(fundName)
                    )
                    val fundDetailsToSave = FundDetails(
                        totalExpectedDeposit = existingFund?.totalExpectedDeposit ?: 0.0,
                        totalCurrentDeposit = existingFund?.totalCurrentDeposit ?: 0.0,
                        totalCurrentLateFee = existingFund?.totalCurrentLateFee ?: 0.0,
                        totalCurrentInterestCollected = existingFund?.totalCurrentInterestCollected
                            ?: 0.0,
                        totalExpectedMaturityAmount = existingFund?.totalExpectedMaturityAmount
                            ?: 0.0, // defaulted as no loans interest or fee, this will change
                        totalCurrAmount = existingFund?.totalCurrAmount ?: 0.0,
                        fundDetailsId = existingFund?.fundDetailsId ?: 0,
                        fundId = existingFund?.fundId ?: 0

                    )
                    val newFundId = withContext(Dispatchers.IO) {
                        fundViewModel.saveOrUpdateFund(
                            fundToSave,
                            fundDetailsToSave,
                            groupId,
                            recalculateFinance,
                            excludeCreator
                        )
                    }
                    fundSharedViewModel.refreshFunds()
                    if (needsTabRefresh && SessionManager.canManageDeposits()) {
                        sharedAdminViewModel.refreshAdminTabs()
                    }
                    binding.noFundMessageContainer.visibility = View.GONE
                    if (fundDetailsToSave.totalCurrentDeposit > 0) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.message_fund_update_success),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        if (!excludeCreator) {

                            val role =
                                if (moderator == SessionManager.userId) {
                                    ApnaBankConstants.ROLE_PRIMARY_MODERATOR
                                } else {
                                    ApnaBankConstants.ROLE_MODERATOR
                                }

                            SessionManager.addFundMembership(
                                FundMembers(
                                    userId = SessionManager.userId,
                                    fundId = newFundId,
                                    role = role,
                                    status = ApnaBankConstants.STATUS_ACTIVE,
                                    joiningDate = ApnaBankDate.getCurrentDate(),
                                    updatedBy = SessionManager.userId
                                ).toUserFundMembership()
                            )

                            AlertDialog.Builder(requireContext())
                                .setTitle(getString(R.string.title_fund_update_created_success))
                                .setMessage(getString(R.string.message_want_add_update_members_now))
                                .setPositiveButton(getString(R.string.text_yes)) { _, _ ->

                                    val action = FundFragmentDirections
                                        .actionFundFragmentToFundMemberDetailsFragment(
                                            fundId = newFundId
                                        )

                                    findNavController().navigate(action)
                                }
                                .setNegativeButton(
                                    getString(R.string.text_cancel_button),
                                    null
                                )
                                .show()

                        } else {

                            Toast.makeText(
                                requireContext(),
                                getString(R.string.title_fund_update_created_success),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    dialog.dismiss()
                } catch (e: Exception) {
                    Log.e("FundFragment", e.message.toString())
                    dialogBinding.buttonSaveFund.isEnabled = true
                    dialogBinding.buttonSaveFund.text = getString(R.string.text_save_button)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_server),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }
        dialogBinding.buttonCancelFund.setOnClickListener {
            dialog.dismiss()
        }
    }

    fun TextInputLayout.setInfoDialog(
        titleRes: Int,
        messageRes: Int
    ) {
        setEndIconOnClickListener {
            AlertDialog.Builder(context)
                .setTitle(context.getString(titleRes))
                .setMessage(context.getString(messageRes))
                .setPositiveButton(context.getString(R.string.text_button_ok), null)
                .show()
        }
    }

    fun DialogAddFundBinding.setupForm() {

        // Required fields
        listOf(
            inputLayoutFundName,
            inputLayoutPeriod,
            inputLayoutStartDate,
            inputLayoutDepositLastDate,
            inputLayoutLateFeeRate,
            textInputDepAmount,
            inputLayoutModerator,
            inputLayoutLoanIntRate,
            inputLayoutGroup
        ).forEach { it.markRequired() }

        // Info dialogs
        val infoFields = mapOf(
            inputLayoutPeriod to Pair(
                R.string.title_fund_period_info,
                R.string.info_fund_period
            ),
            inputLayoutLoanIntRate to Pair(
                R.string.title_fund_loan_int_rate_info,
                R.string.info_fund_loan_int_rate
            ),
            inputLayoutRevisedLoanInterestRate to Pair(
                R.string.title_revised_loan_interest_rate_info,
                R.string.info_revised_loan_interest_rate
            ),
            inputLayoutInterestRateRevisionMonths to Pair(
                R.string.title_interest_rate_revision_months_info,
                R.string.info_interest_rate_revision_months
            ),
            inputLayoutLateFeeRate to Pair(
                R.string.title_fund_late_fee_rate_info,
                R.string.info_fund_late_fee_rate
            ),
            inputLayoutDepositLastDate to Pair(
                R.string.title_fund_monthly_deposit_last_day_info,
                R.string.info_fund_monthly_deposit_last_day
            ),
            inputLayoutModerator to Pair(
                R.string.title_fund_moderator_info,
                R.string.info_fund_moderator
            ),
            inputLayoutDepFrequency to Pair(
                R.string.title_fund_dep_frequency_info,
                R.string.info_fund_dep_frequency
            )
        )

        infoFields.forEach { (view, data) ->
            view.setInfoDialog(data.first, data.second)
        }
    }

    private fun updateSaveButtonState(
        dialogBinding: DialogAddFundBinding,
        groupId: Int,
        moderator: Int
    ) {
        Log.d(
            "SAVE_DEBUG",
            "groupId=$groupId moderator=$moderator"
        )
        dialogBinding.buttonSaveFund.isEnabled =
            validateInputs(dialogBinding, groupId, moderator)

        if (dialogBinding.buttonSaveFund.isEnabled) {
            dialogBinding.buttonSaveFund.isClickable = true
        }
    }


    private fun validateInputs(
        dialogBinding: DialogAddFundBinding,
        groupId: Int,
        moderator: Int
    ): Boolean {
        Log.d(
            "SAVE_DEBUG",
            "validate groupId=$groupId moderator=$moderator"
        )
        return FundInputValidator.isAllInputValid(
            fundName = dialogBinding.editTextFundName.text?.toString(),
            startDate = dialogBinding.editTextFundStartDate.text?.toString(),
            period = dialogBinding.editTextPeriod.text?.toString(),
            depFreq = dialogBinding.editTextDepFrequency.text?.toString(),
            depAmount = dialogBinding.editTextDepAmount.text?.toString(),
            loanRate = dialogBinding.editTextLoanIntRate.text?.toString(),
            lateFee = dialogBinding.editTextLateFeeRate.text?.toString(),
            depLastDate = dialogBinding.editTextDepositLastDate.text?.toString(),
            hasVariableInterestRate = dialogBinding.checkBoxVariableInterestRate.isChecked,
            revisedLoanInterestRate = dialogBinding.editTextRevisedLoanInterestRate.text?.toString(),
            interestRateRevisionAfterMonths = dialogBinding.editTextInterestRateRevisionMonths.text?.toString(),
            groupId = groupId,
            moderator = moderator
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchAllFunds(adapter: FundAdapter): List<FundWithDetails> {
        val sortedFundWithDetails: List<FundWithDetails>
        val fundsWithDetails: List<FundWithDetails> = fundViewModel.fetchAllFundsWithDetails(
            isAdmin,
            moderatorGroupId
        )
        // Sort the funds based on status and then start dates
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        sortedFundWithDetails = withContext(Dispatchers.Default) {
            fundsWithDetails.sortedWith(
                compareBy<FundWithDetails> { it.fundStatus != "ACTIVE" }
                    .thenByDescending { LocalDate.parse(it.fundStartDate, formatter) }
            )
        }
        adapter.setFundsWithDetails(sortedFundWithDetails)

        return sortedFundWithDetails
    }

    private fun showDatePickerDialog(targetEditText: EditText) {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                // Format and set the date (dd/MM/yyyy)
                val dateStr = String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%04d",
                    dayOfMonth,
                    month + 1,
                    year
                )
                targetEditText.setText(dateStr)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showApplyLoanDialog(
        borrowerUserId: Int? = null,
        fundId: Int,
        borrowerName: String? = null,
        existingLoan: LoanDetailsWithMemberNames? = null,
        allowEditLoan: Boolean = true
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val fund = userSummaryViewModel.getFund(fundId)!!
            val fundRateOfInterest = fund.loanInterestRate
            val members = fundViewModel.getFundMembersWithNamesForFund(fundId)
            val borrowerList = members.map { it.userId to "${it.firstName} ${it.lastName}" }
            //Get current available amount in the Fund display to the user and also add a validation
            val totalAmounts = userSummaryViewModel.getAllAmountAvailableforFund(fundId)
            val dialog = AddLoanDialog(
                borrowerName = borrowerName,
                rateOfInterest = fundRateOfInterest,
                fundMaturityDate = fund.fundMaturityDate,
                fundStartDate = fund.fundStartDate,
                existingLoan = existingLoan,
                allowEditLoan = allowEditLoan,
                borrowerUserId = borrowerUserId,
                totalAmounts = totalAmounts,
                borrowerList = borrowerList
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
                    showSuccessSnackbar(
                        getString(R.string.message_loan_created_approved)
                    )
                }
            }
            dialog.show(parentFragmentManager, ApnaBankConstants.TEXT_ADD_LOAN_DIALOG)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setupModeratorDropdown(
        dialogBinding: DialogAddFundBinding,
        members: List<UserDisplay>,
        onModeratorSelected: (Int) -> Unit
    ) {
        dialogBinding.editTextModerator.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = members[position]
            onModeratorSelected(selectedUser.userId)
        }

        dialogBinding.editTextModerator.setOnClickListener {
            dialogBinding.editTextModerator.showDropDown()
        }

        dialogBinding.editTextModerator.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                dialogBinding.editTextModerator.post {
                    dialogBinding.editTextModerator.showDropDown()
                }
            }
        }
    }

    private fun refreshModeratorDropdown(
        groupId: Int,
        dialogBinding: DialogAddFundBinding,
        context: Context,
        existingFund: FundWithDetails?,
        defaultModerator: Int?,
        onDataReady: (List<UserDisplay>, Int?) -> Unit
    ) {
        lifecycleScope.launch {

            val members: List<UserDisplay> = if (existingFund == null) {
                // ADD → group members
                groupViewModel.fetchGroupMembersforGrpWithNames(groupId, true)
                    .map {
                        UserDisplay(
                            userId = it.userId,
                            displayName = "${it.firstName} ${it.lastName}"
                        )
                    }
            } else {
                // EDIT → fund members
                fundViewModel.getFundMembersWithNamesForFund(existingFund.fundId)
                    .map {
                        UserDisplay(
                            userId = it.userId,
                            displayName = "${it.firstName} ${it.lastName}"
                        )
                    }
            }
            if (members.isEmpty()) {
                dialogBinding.editTextModerator.setText("", false)
                dialogBinding.editTextModerator.setAdapter(null)
                return@launch
            }
            val nameMap = members.associateBy { it.displayName }
            val nameList = nameMap.keys.toList()

            val adapter = ArrayAdapter(context, R.layout.dropdown_item_apnabank, nameList)
            dialogBinding.editTextModerator.setAdapter(adapter)

            // Pre-select if editing existing fund
            val preselectUserId = when {
                existingFund != null -> existingFund.moderator

                members.any { it.userId == SessionManager.userId } ->
                    SessionManager.userId

                else -> defaultModerator
            }
            val preselectUser = members.find { it.userId == preselectUserId }
            if (preselectUser != null) {
                val name = preselectUser.displayName
                dialogBinding.editTextModerator.setText(name, false)


            }
            if (members.size == 1) {
                val singleUser = members.first()

                dialogBinding.editTextModerator.setText(singleUser.displayName, false)

                // Disable interaction
                dialogBinding.editTextModerator.isEnabled = false
                dialogBinding.editTextModerator.isClickable = false


                // still notify selection
                onDataReady(
                    members,
                    preselectUser?.userId ?: singleUser.userId
                )

                return@launch
            } else {
                // Enable interaction for multiple users
                dialogBinding.editTextModerator.isEnabled = true
                dialogBinding.editTextModerator.isClickable = true
                dialogBinding.editTextModerator.isFocusable = false
                dialogBinding.editTextModerator.isFocusableInTouchMode = false
            }
            onDataReady(members,preselectUser?.userId )
        }
    }

    fun TextInputLayout.markRequired() {
        val label = this.hint?.toString() ?: ""
        val spannable = SpannableString("$label *")
        spannable.setSpan(
            ForegroundColorSpan(Color.RED),
            spannable.length - 1,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        this.hint = spannable
    }
}