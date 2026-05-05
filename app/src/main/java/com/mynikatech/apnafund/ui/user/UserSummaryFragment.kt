package com.mynikatech.apnafund.ui.user

import android.app.AlertDialog
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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.TooltipCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.mynikatech.apnafund.Exception.InvalidSessionException
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.FundWithDetails
import com.mynikatech.apnafund.data.model.Groups
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.data.model.UserProfile
import com.mynikatech.apnafund.databinding.DialogAddGroupBinding
import com.mynikatech.apnafund.databinding.FragmentUserSummaryBinding
import com.mynikatech.apnafund.session.PreferencesHelper
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.loan.AddLoanDialog
import com.mynikatech.apnafund.ui.loan.LoanAdapter
import com.mynikatech.apnafund.ui.viewmodel.FundSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.GroupViewModel
import com.mynikatech.apnafund.ui.viewmodel.LoansViewModel
import com.mynikatech.apnafund.ui.viewmodel.NotificationSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.StartupViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserSummaryViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.ApnaBankDate
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.Converters.toTitleCase
import com.mynikatech.apnafund.util.GroupInputValidator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.compareTo

class UserSummaryFragment : Fragment() {

    private lateinit var binding: FragmentUserSummaryBinding

    private val userSummaryViewModel: UserSummaryViewModel by viewModels()

    private val userViewModel: UserViewModel by viewModels()

    private val loanViewModel: LoansViewModel by viewModels()

    private val groupViewModel: GroupViewModel by viewModels()

    private val fundSharedViewModel: FundSharedViewModel by activityViewModels()

    private val notificationViewModel: NotificationSharedViewModel by activityViewModels()

    private var adapter: LoanAdapter? = null

    private var isFundExpanded = false

    private var totalDepositAmount: Double? = null

    private var totalMaturityAmount: Double? = null

    private var userId = -1

    private val startUpViewModel: StartupViewModel by viewModels()

    private val firebaseAuthListener = FirebaseAuth.AuthStateListener { auth ->

        val firebaseUser = auth.currentUser

        if (firebaseUser != null) {

            SessionManager.firebaseUid = firebaseUser.uid

            Log.d(
                "FirebaseAuth",
                "Firebase UID ready: ${SessionManager.firebaseUid}"
            )

            viewLifecycleOwner.lifecycleScope.launch {
                userSummaryViewModel.syncFirebaseUidIfNeeded()
            }

        } else {
            Log.w("FirebaseAuth", "Firebase user not ready yet")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        Log.d("UserSummary", "On Resume called")
        Log.d("UserSummary", "The group name in session is ${SessionManager.groupName}")
        /*binding.textViewWelcomeMessage.text =
            getString(R.string.text_welcome_message, SessionManager.userName)*/
        refreshGroupChip()
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

        Log.d("UserSummary", "On View Created called")

        // Session invalidation observer
        viewLifecycleOwner.lifecycleScope.launch {
            userSummaryViewModel.sessionInvalid.collect {
                forceLogout()
            }
        }

        val argUserId = arguments?.getInt("userId", -1) ?: -1

        userId = if (argUserId > 0) {
            argUserId
        } else {
            SessionManager.userId
        }
        TooltipCompat.setTooltipText(
            binding.fabAssistant,
            getString(R.string.title_ask_Maya)
        )
        Log.d("ApnaFund", "UserFragment: The user Id is $userId")

        binding.fabAssistant.setOnClickListener {
            findNavController().navigate(R.id.aiChatFragment)
        }
        setupObservers()

        userSummaryViewModel.userName.observe(viewLifecycleOwner) { nameFromVm ->

            val preferred = SessionManager.getFormattedUserName()

            val display = preferred.ifBlank { nameFromVm }
            Log.d("UserSummary", " User Name Observing: and display Name is $display")
            binding.textViewWelcomeMessage.text =
                getString(R.string.text_welcome_message, display)
        }

        /* ---------------- LOAD USER PROFILE ---------------- */

        viewLifecycleOwner.lifecycleScope.launch {
            try {

                val userProfile = userSummaryViewModel.getUserProfile(userId)
                if (!isAdded) return@launch
                saveUserProfilesSession(userProfile)
                val newName = SessionManager.getFormattedUserName()
                if (userSummaryViewModel.userName.value.isNullOrBlank()) {
                    userSummaryViewModel.userName.value = newName
                }
                refreshGroupChip()
                ensureFirebaseLogin()

                notificationViewModel.loadUnreadCount(SessionManager.userId)

            } catch (_: InvalidSessionException) {
                // already handled
            } catch (e: Exception) {

                Log.e("UserSummary", "Failed to load profile", e)

                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_failed_load_profile),
                    Toast.LENGTH_LONG
                ).show()
            }
            if (!isAdded) return@launch
            val roleCodes = SessionManager.roleNames

            val activity = activity ?: return@launch
            val bottomNav =
                activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
            val isOnlyMember =
                roleCodes.contains("MEMBER") && roleCodes.size == 1

            bottomNav.menu
                .findItem(R.id.adminFragment)
                ?.isVisible = !isOnlyMember

            val hasGroup = (SessionManager.groupId ?: 0) > 0
            val isAdmin = roleCodes.contains("ADMIN")
            val isModerator = SessionManager.isModerator()
            val pendingGroups = SessionManager.userPendingGroups ?: emptyList()
            val activeGroups = SessionManager.userGroups ?: emptyList()
            val hasActive = activeGroups.isNotEmpty()
            val hasPending = pendingGroups.isNotEmpty()

            val canAccessGroupTabs = hasActive || isAdmin

            bottomNav.menu.findItem(R.id.groupChatFragment)?.isVisible = canAccessGroupTabs
            bottomNav.menu.findItem(R.id.fundFragment)?.isVisible = canAccessGroupTabs

            bottomNav.menu.findItem(R.id.adminFragment)?.isVisible =
                (isAdmin || isModerator) && canAccessGroupTabs

            findNavController().currentBackStackEntry
                ?.savedStateHandle
                ?.getLiveData<Boolean>("profile_updated")
                ?.observe(viewLifecycleOwner) {

                }

            if (!hasActive) {

                // Show empty/pending container
                binding.noGroupMessageContainer.visibility = View.VISIBLE
                binding.GroupMessageContainer.visibility = View.GONE

                // Toggle message inside container
                binding.textViewNoGroupMessage.visibility =
                    if (!hasPending) View.VISIBLE else View.GONE

                binding.textViewPendingMessage.visibility =
                    if (hasPending) View.VISIBLE else View.GONE
            } else {

                binding.noGroupMessageContainer.visibility = View.GONE
                binding.GroupMessageContainer.visibility = View.VISIBLE

                lazyLoadContent()
            }
            fun handleAddGroupClick() {

                when {
                    hasPending && pendingGroups.size >= 3 -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.msg_pending_limit),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    hasPending -> {
                        showPendingWarningDialog {
                            showAddGroupDialog(requireContext(), 0)
                        }
                    }

                    else -> {
                        showAddGroupDialog(requireContext(), 0)
                    }
                }
            }
            binding.layoutAddGroup.setOnClickListener {
                handleAddGroupClick()
            }
            binding.buttonAddGroupNone.setOnClickListener {
                handleAddGroupClick()
            }
        }

    }

    fun showPendingWarningDialog(onContinue: () -> Unit) {
        if (!isAdded) return

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.header_add_group))
            .setMessage(getString(R.string.msg_pending_warning))
            .setPositiveButton(getString(R.string.label_continue)) { dialog, _ ->
                dialog.dismiss()
                onContinue()
            }
            .setNegativeButton(getString(R.string.text_cancel_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun lazyLoadContent() {
        val groupId = SessionManager.groupId ?: return
        val isAdmin = SessionManager.roleNames.contains("ADMIN")

        fundSharedViewModel.loadFunds(isAdmin, groupId)
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
                    loanDetails.loanId?.let { loanId ->
                        val action = UserSummaryFragmentDirections
                            .actionUserSummaryFragmentToLoanEmiFragment(loanId)
                        findNavController().navigate(action)
                    } ?: run {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_loan_id_not_available),
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.w("UserSummary", "Loan EMI click attempted with null loanId")
                    }
                },
                onCloseLoanClick = { loan ->
                    val isModerator = SessionManager.isModerator()

                    if (isModerator) {
                        showDirectClosureConfirmation(loan)
                    } else {
                        checkPendingAndProceed(loan)
                    }
                }
            )
            binding.recyclerViewLoans.adapter = adapter
        }
        try {
            userSummaryViewModel.loadUserSummary(userId, SessionManager.groupId)

        } catch (e: Exception) {

            Log.e("UserSummary", "Failed to load summary", e)

            Toast.makeText(
                requireContext(),
                getString(R.string.error_unable_oad_data),
                Toast.LENGTH_SHORT
            ).show()
        }
        userSummaryViewModel.groupName.observe(viewLifecycleOwner) {
            binding.chipGroupName.text = getString(
                R.string.header_group,
                SessionManager.groupName ?: "--"
            )

            binding.chipGroupName.isClickable =
                SessionManager.isMultiGroupUser()

        }

        loanViewModel.closeLoanResult.observe(viewLifecycleOwner) { result ->

            result.onSuccess {
                Toast.makeText(requireContext(), "Loan closed successfully", Toast.LENGTH_SHORT).show()
            }

            result.onFailure {
                Toast.makeText(requireContext(), it.message ?: "Failed", Toast.LENGTH_SHORT).show()
            }
        }
        binding.chipGroupName.setOnClickListener {

            val groups = SessionManager.userGroups ?: emptyList()

            if (groups.size <= 1) {
                val groupId = SessionManager.groupId ?: return@setOnClickListener
                val action = UserSummaryFragmentDirections
                    .actionUserSummaryFragmentToGroupDetailsFragment(groupId)
                findNavController().navigate(action)
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.text_select_group))
                .setItems(groups.map { it.groupName }.toTypedArray()) { _, which ->

                    val selected = groups[which]

                    if (selected.groupId == SessionManager.groupId) return@setItems

                    SessionManager.setSelectedGroup(selected)

                    binding.chipGroupName.setText(
                        getString(R.string.group_label, selected.groupName)
                    )

                    reloadForSelectedGroup()
                }
                .show()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                fundSharedViewModel.funds.collect { funds ->
                    handleFundsUI(funds)
                }
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
                fundSharedViewModel.setSelectedFundId(fund.fundId)
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

            }

        }
        binding.recyclerViewLoans.apply {
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(context).apply {
                isItemPrefetchEnabled = false
            }
            setHasFixedSize(false)
        }
        binding.recyclerViewLoans.post {
            binding.recyclerViewLoans.requestLayout()
        }
        userSummaryViewModel.userLoans.observe(viewLifecycleOwner) { loans ->
            val hasLoans = !loans.isNullOrEmpty()
            val selectedFund = fundSharedViewModel.selectedFund.value
            val isClosedFund =
                selectedFund?.fundStatus == ApnaBankConstants.STATUS_CLOSED ||
                        selectedFund?.fundStatus == ApnaBankConstants.INACTIVE_STATUS

            binding.recyclerViewLoans.visibility = if (hasLoans) View.VISIBLE else View.GONE
            binding.textViewNoLoans.visibility =
                if (!hasLoans && !isClosedFund) View.VISIBLE else View.GONE
            binding.textViewLoansHeader.text = getString(R.string.text_loan)
            adapter?.setLoans(loans ?: emptyList())
        }

        binding.imageFundExpandCollapse.setOnClickListener {
            isFundExpanded = !isFundExpanded
            updateFundExpandUI()
            if (isFundExpanded) {
                binding.recyclerViewLoans.post {
                    // Key fix
                    binding.recyclerViewLoans.layoutParams.height =
                        ViewGroup.LayoutParams.WRAP_CONTENT

                    binding.recyclerViewLoans.requestLayout()
                    binding.recyclerViewLoans.invalidate()
                }
            }
        }
        binding.loanAddGroup.setOnClickListener {
            val fundId = fundSharedViewModel.selectedFundId.value

            if (fundId != null) {
                showApplyLoanDialog(
                    userId,
                    fundId,
                    borrowerName = SessionManager.getFormattedUserName()
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.message_select_valid_fund), Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun showDirectClosureConfirmation(loan: LoanDetailsWithMemberNames) {

        val message = getString(R.string.message_close_loan_direct, loan.loanNumber,  Converters.formatCurrency(loan.currPrincipal))

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_close_loan_moderator))
            .setMessage(message)
            .setPositiveButton(getString(R.string.text_close_loan)) { _, _ ->
                loanViewModel.closeLoanDirect(loan.loanId ?: return@setPositiveButton, SessionManager.userId)
            }
            .setNegativeButton(getString(R.string.text_cancel_button), null)
            .show()
    }

    private fun updateFundExpandUI() {
        binding.lineFundDetails.visibility =
            if (isFundExpanded) View.VISIBLE else View.GONE

        binding.fundDetailsSection.visibility =
            if (isFundExpanded) View.VISIBLE else View.GONE

        binding.imageFundExpandCollapse.setImageResource(
            if (isFundExpanded) R.drawable.ic_up_arrow
            else R.drawable.ic_down_arrow
        )
    }

    private fun showCloseLoanConfirmation(loan: LoanDetailsWithMemberNames) {

        val tentativeClosureAmount =
            (loan.currPrincipal) + (loan.emiInterest)

        val message = getString(
            R.string.message_close_loan_request,
            loan.loanNumber,
            Converters.formatCurrency(loan.loanAmount),
            Converters.formatCurrency(loan.currPrincipal),
            Converters.formatCurrency(loan.emiInterest),
            Converters.formatCurrency(tentativeClosureAmount)
        )

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.text_close_loan))
            .setMessage(message)
            .setPositiveButton(getString(R.string.text_button_ok)) { _, _ ->
                raiseLoanClosureRequest(loan)
            }
            .setNegativeButton(getString(R.string.text_cancel_button), null)
            .show()
    }

    private fun updateUserSummary() {

        val deposit = totalDepositAmount ?: 0.0
        val maturity = totalMaturityAmount ?: 0.0

        binding.textViewFundSummaryValue.text =
            getString(
                R.string.text_total_deposit_maturity,
                Converters.formatCurrency(deposit),
                Converters.formatCurrency(maturity)
            )

        if (deposit == 0.0 && maturity == 0.0) {
            binding.textViewFundSummaryValue.text = getString(R.string.text_no_fund_activity)
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
            val fund = userSummaryViewModel.getFund(fundId)

            if (fund == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.warn_fund_not_found), Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            //Get current available amount in the Fund display to the user and also add a validation
            val totalAmounts = userSummaryViewModel.getAllAmountAvailableforFund(fundId)

            val dialog = AddLoanDialog(
                borrowerName = borrowerName,
                rateOfInterest = fundRateOfInterest,
                fundMaturityDate = fund.fundMaturityDate,
                fundStartDate = fund.fundStartDate,
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
                        autoapprove = false,
                        requestorId = SessionManager.userId

                    )
                }
            }
            dialog.show(parentFragmentManager, "AddLoanDialog")
        }
    }

    fun saveUserProfilesSession(userProfiles: UserProfile) {
        Log.d(
            "FirebaseAuth",
            "Session Manager firebase on profile setting: ${SessionManager.firebaseUid}"
        )
        val userId = userProfiles.userId
        val userName = userProfiles.userName

        val groups = userProfiles.groups
        val pendingGroups = userProfiles.pendingGroups
        SessionManager.userGroups = groups
        SessionManager.userPendingGroups = pendingGroups
        if (groups.isEmpty()) {
            // Admin or user without group
            SessionManager.groupId = null
            SessionManager.groupName = null
        } else if (SessionManager.groupId == null) {
            // First login or session not yet initialized
            SessionManager.setSelectedGroup(groups.first())
        }

        val firstName = userProfiles.firstName
        val lastName = userProfiles.lastName
        val emailId = userProfiles.emailId
        val phoneNumber = userProfiles.phoneNumber

        val roleIds = userProfiles.roles.mapNotNull { it.roleId }.distinct()
        val roleNames = userProfiles.roles.map { it.roleCode }.distinct()
        val selectedGroupId = SessionManager.groupId
        val selectedGroupName = SessionManager.groupName
        val prefsHelper = PreferencesHelper(requireContext())
        prefsHelper.saveSession(
            userId = userId,
            userName = userName,
            roleIds = roleIds,
            roleNames = roleNames,
            groupId = selectedGroupId,
            token = null, // Actual token if available(todo)
            isPinSet = userProfiles.isPinSet ?: false,
            firstName = firstName,
            lastName = lastName ?: "",
            emailId = emailId,
            phoneNumber = phoneNumber,
            firebaseUid = SessionManager.firebaseUid,
            groupName = selectedGroupName ?: "Default Group"
        )
        prefsHelper.loadSession()
    }

    override fun onDestroyView() {
        binding.recyclerViewLoans.adapter = null
        adapter = null
        super.onDestroyView()
    }

    private fun forceLogout() {
        PreferencesHelper(requireContext()).clearSession()
        fundSharedViewModel.clearCache()

        findNavController().navigate(
            R.id.loginFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, inclusive = true)
                .build()
        )
    }

    private fun updateLoanAddVisibility(fundStatus: String?) {
        val canAddLoan =
            fundStatus != null &&
                    Converters.userHasPrivilege(ApnaBankConstants.ADD_APPLY_LOAN_PRIV) &&
                    fundStatus != ApnaBankConstants.INACTIVE_STATUS &&
                    fundStatus != ApnaBankConstants.STATUS_CLOSED

        binding.loanAddFab.visibility = if (canAddLoan) View.VISIBLE else View.GONE
        binding.loanAddText.visibility = if (canAddLoan) View.VISIBLE else View.GONE
    }

    private fun reloadForSelectedGroup() {

        val groupId = SessionManager.groupId ?: return
        val isAdmin = SessionManager.roleNames.contains("ADMIN")

        fundSharedViewModel.clearSelectedFund()
        fundSharedViewModel.clearSelectedFundId()
        fundSharedViewModel.loadFunds(isAdmin, groupId)

        userSummaryViewModel.loadUserSummary(
            userId = SessionManager.userId,
            selectedGroupId = groupId
        )

    }

    private fun refreshGroupChip() {
        binding.chipGroupName.text = getString(
            R.string.header_group,
            SessionManager.groupName ?: "--"
        )
    }

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuthListener)
    }

    override fun onStop() {
        super.onStop()

        FirebaseAuth.getInstance()
            .removeAuthStateListener(firebaseAuthListener)
    }

    private fun ensureFirebaseLogin() {

        Log.d("Firebase", "Called ensureFirebaseLogin")

        val firebaseUser = FirebaseAuth.getInstance().currentUser

        // If already logged in → nothing to do
        if (firebaseUser != null) {
            Log.d("Firebase", "Firebase already logged in")
            return
        }
        Log.d("Firebase", "Session Email Id ${SessionManager.emailId}")
        val email = SessionManager.emailId

        if (email.isBlank()) {
            Log.w("Firebase", "Email missing in session — cannot restore Firebase session")
            return
        }

        lifecycleScope.launch {
            Log.d("Firebase", "Restoring Firebase session for $email")
            try {
                startUpViewModel.restoreFirebaseSession(email)
            } catch (e: Exception) {

                Log.e("Firebase", "Failed to restore Firebase session", e)
            }
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

    fun DialogAddGroupBinding.setupForm() {

        // Required fields
        listOf(
            textInputGroupName,
            textInputAutoModerator,
        ).forEach { it.markRequired() }

        // Info dialogs
        val infoFields = mapOf(
            textInputGroupName to Pair(
                R.string.title_user_group_info,
                R.string.info_user_group
            ),
            textInputAutoModerator to Pair(
                R.string.title_group_moderator_info,
                R.string.info_group_moderator
            ),
            textInputGroupDesc to Pair(
                R.string.title_group_desc_info,
                R.string.info_group_desc
            )
        )

        infoFields.forEach { (view, data) ->
            view.setInfoDialog(data.first, data.second)
        }
    }

    private fun showAddGroupDialog(
        context: Context,
        addOrEditFlag: Int,
        existingGroup: Groups? = null
    ) {
        val dialogBinding = DialogAddGroupBinding.inflate(LayoutInflater.from(context))
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(dialogBinding.root)
        dialog.show()
        val isAdmin = SessionManager.isAdmin()
        var userId = 0
        dialogBinding.setupForm()
        dialogBinding.buttonSaveGrp.text =
            if (addOrEditFlag == 1)
                getString(R.string.text_update_group)
            else
                getString(R.string.text_add_group)

        // Pre-fill group name
        if (addOrEditFlag == 1) {
            dialogBinding.editTextGroupName.setText(existingGroup?.groupName)
            dialogBinding.editTextGroupDesc.setText(existingGroup?.description)
        }

        dialogBinding.buttonSaveGrp.isEnabled = false

        // Validation helper
        fun validateInputs() {
            val name = dialogBinding.editTextGroupName.text.toString().trim()
            val isValid = GroupInputValidator.isGroupFormValid(name, userId)
            dialogBinding.buttonSaveGrp.isEnabled = isValid

            if (!GroupInputValidator.isGroupNameValidInput(name) && name.isNotEmpty()) {
                dialogBinding.editTextGroupName.error =
                    getString(R.string.error_group_name_3_characters)
            }
        }

        dialogBinding.editTextGroupName.addTextChangedListener {
            validateInputs()
        }

        // Load user list and handle selection
        // when the user is admin all the users should be made available
        // however when the user is not admin only the user should be available

        lifecycleScope.launch {
            try {
                userViewModel.fetchUsers().collectLatest { users ->

                    if (!isAdmin) {
                        // Moderator → only themselves
                        val currentUser = users.find { it.userId == SessionManager.userId }

                        val fullName = "${currentUser?.firstName} ${currentUser?.lastName}"

                        dialogBinding.editTextAutoModerator.setText(fullName, false)
                        userId = currentUser?.userId ?: 0

                        dialogBinding.editTextAutoModerator.isEnabled = false
                        dialogBinding.editTextAutoModerator.isClickable = false
                        dialogBinding.editTextAutoModerator.isFocusable = false

                        validateInputs()

                    } else {

                        // Admin → show all users
                        val userNames = users.map { "${it.firstName} ${it.lastName}" }

                        val adapter = ArrayAdapter(
                            requireContext(),
                            R.layout.dropdown_item_apnabank,
                            userNames
                        )

                        dialogBinding.editTextAutoModerator.setAdapter(adapter)

                        // Preselect existing moderator if editing
                        if (addOrEditFlag == 1 && existingGroup != null) {
                            val existingModerator =
                                users.find { it.userId == existingGroup.moderator }
                            val fullName =
                                "${existingModerator?.firstName} ${existingModerator?.lastName}"

                            dialogBinding.editTextAutoModerator.setText(fullName, false)
                            userId = existingModerator?.userId ?: 0
                            validateInputs()
                        }

                        dialogBinding.editTextAutoModerator.setOnClickListener {
                            dialogBinding.editTextAutoModerator.showDropDown()
                        }

                        dialogBinding.editTextAutoModerator.setOnFocusChangeListener { _, hasFocus ->
                            if (hasFocus) {
                                dialogBinding.editTextAutoModerator.post {
                                    dialogBinding.editTextAutoModerator.showDropDown()
                                }
                            }
                        }

                        dialogBinding.editTextAutoModerator.setOnItemClickListener { parent, _, position, _ ->
                            val selectedName = parent.getItemAtPosition(position) as String
                            val selectedUser =
                                users.find { "${it.firstName} ${it.lastName}" == selectedName }

                            userId = selectedUser?.userId ?: 0
                            validateInputs()
                        }
                    }
                }
            } catch (e: Exception) {

                Log.e("AddGroupDialog", "Failed to load users", e)

                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_failed_to_load_users),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialogBinding.buttonSaveGrp.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {

                val groupName = dialogBinding.editTextGroupName.text.toString().trim()
                    .toTitleCase()
                val status = if (isAdmin) {
                    ApnaBankConstants.STATUS_ACTIVE
                } else {
                    ApnaBankConstants.STATUS_PENDING
                }
                val groupToSave = existingGroup?.copy(
                    groupName = groupName,
                    moderator = userId,
                    status = existingGroup.status,
                    description = dialogBinding.editTextGroupDesc.text.toString().trim()
                ) ?: Groups(
                    groupName = dialogBinding.editTextGroupName.text.toString().trim()
                        .toTitleCase(),
                    moderator = userId,
                    status = status,
                    createdDate = ApnaBankDate.getCurrentDate(),
                    description = dialogBinding.editTextGroupDesc.text.toString().trim(),
                    groupCode = Converters.generateGroupCode(groupName)
                )
                val savedGroup = groupViewModel.saveOrUpdateGroup(groupToSave)
                if (savedGroup != null) {

                    val message = if (isAdmin) {
                        getString(R.string.message_group_created_success)
                    } else {
                        getString(R.string.message_group_request_submitted_for_approval)
                    }

                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

                } else {

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_failed_to_save_group),
                        Toast.LENGTH_LONG
                    ).show()
                }
                dialog.dismiss()
            }
        }
        dialogBinding.buttonCancelGrp.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun handleFundsUI(funds: List<FundWithDetails>) {

        val fundNames = funds.map { it.fundName }

        if (funds.isNotEmpty()) {
            binding.noFundsMessageContainer.visibility = View.GONE
            binding.FundMessageContainer.visibility = View.VISIBLE
            binding.loadingFunds.visibility = View.GONE

            val selectedFundId = fundSharedViewModel.selectedFundId.value
            val fundToUse = funds.firstOrNull { it.fundId == selectedFundId } ?: funds.first()

            fundSharedViewModel.setSelectedFundId(fundToUse.fundId)

            binding.textViewSelectedFund.text = fundToUse.fundName

            val fundStatus = fundToUse.fundStatus

            val isInactive =
                fundStatus == ApnaBankConstants.INACTIVE_STATUS ||
                        fundStatus == ApnaBankConstants.STATUS_CLOSED

            binding.imageFundStatus.setImageResource(
                if (isInactive) R.drawable.status_inactive_dot
                else R.drawable.status_active_dot
            )

            updateLoanAddVisibility(fundStatus)

            userSummaryViewModel.loadFundDetails(userId, fundToUse.fundId)

        } else {
            binding.FundMessageContainer.visibility = View.GONE
            binding.noFundsMessageContainer.visibility = View.VISIBLE
            binding.loadingFunds.visibility = View.GONE
        }
        updateFundExpandUI()

        // 👇 IMPORTANT: use funds directly (not userFunds)
        binding.layoutFundSelector.setOnClickListener {
            if (funds.isEmpty()) return@setOnClickListener

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.text_select_fund))
                .setItems(fundNames.toTypedArray()) { _, which ->

                    val selectedFund = funds.getOrNull(which) ?: return@setItems

                    binding.textViewSelectedFund.text = selectedFund.fundName

                    val isInactive =
                        selectedFund.fundStatus == ApnaBankConstants.INACTIVE_STATUS ||
                                selectedFund.fundStatus == ApnaBankConstants.STATUS_CLOSED

                    binding.imageFundStatus.setImageResource(
                        if (isInactive) R.drawable.status_inactive_dot
                        else R.drawable.status_active_dot
                    )

                    updateLoanAddVisibility(selectedFund.fundStatus)

                    fundSharedViewModel.setSelectedFundId(selectedFund.fundId)

                    userSummaryViewModel.loadFundDetails(userId, selectedFund.fundId)
                }
                .show()
        }
    }
    private fun raiseLoanClosureRequest(loan: LoanDetailsWithMemberNames) {
        loan.loanId?.let { loanId ->
            loanViewModel.requestLoanClosure(
                loanId = loanId,
                requestedAmount = loan.loanAmount, // optional
                remarks = "User requested closure"
            )
        }
    }
    private fun setupObservers() {

        loanViewModel.closureRequestStatus.observe(viewLifecycleOwner) { result ->

            result.onSuccess { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }

            result.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to request closure",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun checkPendingAndProceed(loan: LoanDetailsWithMemberNames) {

        lifecycleScope.launch {

            try {

                val isPending = loanViewModel.hasPendingClosureRequest(loan.loanId ?: 0)
                if (isPending) {

                    AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.title_request_pending))
                        .setMessage(
                            getString(R.string.message_loan_closure_request_pending)
                        )
                        .setPositiveButton(getString(R.string.text_button_ok), null)
                        .show()

                } else {
                    showCloseLoanConfirmation(loan)
                }

            } catch (e: Exception) {
                Log.e("checkPendingAndProceed", e.message.toString())

                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_server),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}