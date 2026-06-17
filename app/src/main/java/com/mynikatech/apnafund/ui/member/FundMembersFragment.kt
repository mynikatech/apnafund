package com.mynikatech.apnafund.ui.member

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.data.model.FundMemberWithName
import com.mynikatech.apnafund.data.model.FundMembers
import com.mynikatech.apnafund.data.model.FundWithDetails
import com.mynikatech.apnafund.databinding.FragmentFundMembersBinding
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.viewmodel.FundSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.FundViewModel
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.Converters.toFundMember
import com.mynikatech.apnafund.util.toUserFundMembership
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FundMembersFragment : Fragment() {

    private lateinit var binding: FragmentFundMembersBinding

    private val fundViewModel: FundViewModel by viewModels()
    private val fundSharedViewModel: FundSharedViewModel by activityViewModels()
    private var groupId: Int = -1
    private var dialog: BottomSheetDialog? = null

    private var fetchMembersJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFundMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let {
            groupId = it.getInt("groupId")
        }
        val navArgsFundId = arguments?.let {
            FundMembersFragmentArgs.fromBundle(it).fundId
        }
        var fundId = 0

        if (navArgsFundId != null) {
            fetchAllMembers(navArgsFundId)
            fundId = navArgsFundId
        } else {
            // Fallback to something to be implemented

        }
        if (SessionManager.canManageFund(fundId))
            binding.fundMembersFab.visibility = View.VISIBLE
        else
            binding.fundMembersFab.visibility = View.GONE
        binding.fundMembersFab.setOnClickListener {
            val action = FundMembersFragmentDirections
                .actionFundMembersFragmentToAddFundMembersFragment(
                    fundId = fundId,
                    groupId = groupId
                )
            findNavController().navigate(action)
        }
        val toolbar = view.findViewById<MaterialToolbar>(R.id.fund_member_details_toolbar)
        // Enable back arrow
        val navController = findNavController()
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow) // your back icon
        toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
        toolbar.title = getString(R.string.header_fund_member_details)
    }

    private fun fetchAllMembers(fundId: Int) {
        // Cancel any ongoing job if you still have that logic (optional now)
        fetchMembersJob?.cancel()
        fetchMembersJob = lifecycleScope.launch {
            val fundMembers = withContext(Dispatchers.IO) {
                fundViewModel.getFundMembersWithNamesForFund(fundId, "ALL")
            }
            val fundDetails = fundViewModel.getFundDetails(fundId)
            if (null != fundDetails)
                groupId = fundDetails.groupId
            if (fundDetails.totalCurrentDeposit > 0)
                binding.fundMembersFab.visibility = View.GONE
            updateTable(fundDetails, fundMembers)
        }
    }

    private fun updateTable(
        fundDetails: FundWithDetails,
        fundMembers: List<FundMemberWithName>
    ) {

        cleanTable(binding.tableFundMembersDetails)

        // Header info
        binding.textViewFundNameValue.text =
            fundDetails.fundName.orEmpty()

        binding.textViewGroupNameValue.text =
            Converters.formatWithBraces(
                fundDetails.groupName.orEmpty()
            )

        binding.textViewModeratorValue.text =
            Converters.formatUserName(
                fundDetails.moderatorFirstName.orEmpty(),
                fundDetails.moderatorLastName.orEmpty()
            )

        val canManageFund =
            SessionManager.canManageFund(fundDetails.fundId)

        val table = binding.tableFundMembersDetails

        fundMembers.forEachIndexed { index, member ->

            val tvNo = TextView(activity).apply {
                text = (index + 1).toString()
                gravity = Gravity.CENTER
            }

            val tvMemberName = TextView(activity).apply {
                text = Converters.formatUserName(
                    member.firstName.orEmpty(),
                    member.lastName.orEmpty()
                )
                gravity = Gravity.START
            }

            val tvJoiningDate = TextView(activity).apply {
                text = member.joiningDate
                gravity = Gravity.START
            }

            val tvRole = TextView(activity).apply {
                text = Converters.toDisplayRole(member.role)
                gravity = Gravity.START
            }

            val tvStatus = TextView(activity).apply {
                text = member.status
                gravity = Gravity.START
            }

            val btnEdit = TextView(activity).apply {
                text = "Edit"
                setTextColor(
                    resources.getColor(R.color.purple)
                )
                setPadding(10, 5, 10, 5)
            }

            val btnToggle = TextView(activity).apply {

                text =
                    if (member.status == "ACTIVE")
                        "Deactivate"
                    else
                        "Activate"

                setTextColor(
                    resources.getColor(R.color.red)
                )

                setPadding(10, 5, 10, 5)
            }

            btnToggle.setOnClickListener {

                val action =
                    if (member.status == "ACTIVE")
                        getString(R.string.action_deactivate)
                    else
                        getString(R.string.action_activate)

                AlertDialog.Builder(requireContext())
                    .setTitle(
                        getString(R.string.title_confirm_action)
                    )
                    .setMessage(
                        getString(
                            R.string.message_confirm_member_status_change,
                            action,
                            member.firstName,
                            member.lastName
                        )
                    )
                    .setNegativeButton(
                        getString(R.string.text_cancel_button),
                        null
                    )
                    .setPositiveButton(action) { _, _ ->

                        handleStatusToggle(
                            member.toFundMember(),
                            fundDetails.fundId
                        )
                    }
                    .show()
            }

            btnEdit.setOnClickListener {
                showRoleChangeDialog(
                    member.toFundMember(),
                    fundDetails.fundId
                )
            }

            btnEdit.visibility =
                if (canManageFund)
                    View.VISIBLE
                else
                    View.GONE

            btnToggle.visibility =
                if (canManageFund)
                    View.VISIBLE
                else
                    View.GONE

            val row = TableRow(activity).apply {

                addView(tvNo)
                addView(tvMemberName)
                addView(tvJoiningDate)
                addView(tvRole)
                addView(tvStatus)
                addView(btnToggle)
                addView(btnEdit)
            }

            table.addView(row)
        }
    }

    private fun handleStatusToggle(
        member: FundMembers,
        fundId: Int
    ) {

        if (member.role == "PRIMARY_MODERATOR") {
            showToast("Cannot modify primary moderator")
            return
        }

        lifecycleScope.launch {

            val fund = fundViewModel.getFundDetails(fundId)
            val hasDeposits = fund.totalCurrentDeposit > 0
            if (hasDeposits) {

                showAlert(
                    getString(R.string.error_cannot_deaxtivate_member_fund)
                )

                return@launch
            }

            val newStatus =
                if (member.status == "ACTIVE")
                    "INACTIVE"
                else
                    "ACTIVE"

            val updatedMember = member.copy(
                status = newStatus,
                updatedBy = SessionManager.userId
            )

            fundViewModel.updateFundMember(
                updatedMember
            )

            if (member.userId == SessionManager.userId) {

                SessionManager.updateFundMembership(
                    updatedMember.toUserFundMembership()
                )
            }
            val isAdmin = SessionManager.roleNames.contains("ADMIN")
            fetchAllMembers(fundId)
            fundSharedViewModel.refreshFunds(fundId)
            fundSharedViewModel.shouldForceRefreshFundDetails = true

        }
    }

    private fun showRoleChangeDialog(member: FundMembers, fundId: Int) {

        if (member.role == "PRIMARY_MODERATOR") {
            showToast("Cannot modify primary moderator")
            return
        }

        val isModerator = member.role == "MODERATOR"

        val optionText = if (isModerator) {
            "Demote to Member"
        } else {
            "Promote to Moderator"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Update Role")
            .setItems(arrayOf(optionText)) { _, _ ->
                val newRole = if (isModerator) "MEMBER" else "MODERATOR"
                handleRoleUpdate(member, fundId, newRole)
            }
            .show()
    }

    private fun handleRoleUpdate(member: FundMembers, fundId: Int, newRole: String) {

        // permission check
        if (newRole == "MODERATOR" && !SessionManager.isPrimaryFundModerator(fundId)) {
            showToast("Only primary moderator can assign moderators")
            return
        }

        val updatedMember = member.copy(
            role = newRole,
            updatedBy = SessionManager.userId
        )
        lifecycleScope.launch {
            fundViewModel.updateFundMember(
                updatedMember
            )
            if (member.userId == SessionManager.userId) {
                SessionManager.updateFundMembership(updatedMember.toUserFundMembership())
            }
            fetchAllMembers(fundId)
        }
    }

    private fun cleanTable(table: TableLayout) {
        while (table.childCount > 1) {
            table.removeViewAt(1)
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith("menu.clear()"))
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        fetchMembersJob?.cancel()
        dialog?.dismiss()
        dialog = null

        cleanTable(binding.tableFundMembersDetails)
        binding.textViewFundNameValue.text = ""
        binding.textViewGroupNameValue.text = ""
        binding.textViewModeratorValue.text = ""

        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.show()
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private fun showAlert(
        message: String,
        title: String = getString(R.string.title_alert)
    ) {

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                getString(R.string.text_button_ok),
                null
            )
            .show()
    }
}