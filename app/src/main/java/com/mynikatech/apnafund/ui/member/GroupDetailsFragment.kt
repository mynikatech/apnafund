package com.mynikatech.apnafund.ui.member

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.databinding.DialogAddGroupMemberBinding
import com.mynikatech.apnafund.databinding.FragmentGroupDetailsBinding
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.viewmodel.GroupSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.GroupViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.toUserGroupMembership
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupDetailsFragment : Fragment() {

    private lateinit var binding: FragmentGroupDetailsBinding
    private lateinit var tableLayoutGroupMemberDetails: TableLayout

    private val groupViewModel: GroupViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val groupSharedViewModel by activityViewModels<GroupSharedViewModel>()
    private var dialog: BottomSheetDialog? = null

    private var fetchMembersJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGroupDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        val navArgsGroupId = arguments?.let {
            GroupDetailsFragmentArgs.fromBundle(it).groupId
        }
        var groupId = 0

        if (navArgsGroupId != null) {
            fetchAllMembers(navArgsGroupId)
            groupId = navArgsGroupId
        } else {
            // Fallback to shared ViewModel
            groupSharedViewModel.selectedGroupId.observe(viewLifecycleOwner) { sharedGroupId ->
                sharedGroupId?.let {
                    fetchAllMembers(it)
                    groupId = it
                }
            }
        }
        if (Converters.userHasPrivilege(ApnaBankConstants.ADD_GROUP_MEMBERS_PRIV) || SessionManager.canManageGroups())
            binding.groupDetailsFab.visibility = View.VISIBLE
        else
            binding.groupDetailsFab.visibility = View.GONE
        binding.groupDetailsFab.setOnClickListener {
            activity?.let { ctx ->
                showAddGroupMemberDialog(ctx, groupId)
            }
        }
        val toolbar = view.findViewById<MaterialToolbar>(R.id.group_details_toolbar)
        // Enable back arrow
        val navController = findNavController()
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow) // your back icon
        toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
        toolbar.title = getString(R.string.text_group_details)
    }

    private fun fetchAllMembers(groupId: Int) {
        // Cancel any ongoing job if you still have that logic (optional now)
        fetchMembersJob?.cancel()
        fetchMembersJob = lifecycleScope.launch {
            val groupMembers = withContext(Dispatchers.IO) {
                groupViewModel.fetchGroupMembersforGrp(groupId)
            }
            updateTable(groupId, groupMembers)
        }
    }

    private suspend fun updateTable(groupId: Int, groupMembers: List<GroupMembers>) {
        if (groupId <= 0) return
        withContext(Dispatchers.Main) {
            cleanTable(binding.tableGroupDetails)

            val userDeferredList = groupMembers.map { member ->
                async { userViewModel.fetchUser(member.userId) }
            }

            val groupName = withContext(Dispatchers.IO) {

                groupViewModel.fetchGroup(groupId)
            }
            binding.textViewGroupNameValue.text = groupName.groupName

            val moderatorUser = withContext(Dispatchers.IO) {
                userViewModel.fetchUser(groupName.moderator!!)
            }
            binding.textViewModeratorValue.text =
                "${moderatorUser?.firstName} ${moderatorUser?.lastName}"

            val canManageGroup =
                SessionManager.canManageGroup(groupId)

            for (i in groupMembers.indices) {
                val member = groupMembers[i]
                val user = userDeferredList[i].await()

                val tvNo = TextView(activity).apply {
                    text = (i + 1).toString()
                    gravity = Gravity.CENTER
                }

                val tvMemberName = TextView(activity).apply {
                    text = "${user?.firstName} ${user?.lastName}"
                    gravity = Gravity.START
                }

                val tvJoiningDate = TextView(activity).apply {
                    text = member.joiningDate
                    gravity = Gravity.START
                }
                val tvRole = TextView(requireContext()).apply {
                    text = Converters.toDisplayRole(member.role)
                    gravity = Gravity.CENTER
                    setPadding(8, 4, 8, 4)
                }

                val tvStatus = TextView(requireContext()).apply {

                    text = member.status
                    gravity = Gravity.CENTER
                    setPadding(8, 4, 8, 4)
                }

                val btnAction = TextView(requireContext()).apply {
                    text = if (member.status == "ACTIVE") "Deactivate" else "Activate"
                    setTextColor(ContextCompat.getColor(context, R.color.purple))
                    setPadding(8, 4, 8, 4)
                }
                btnAction.setOnClickListener {
                    handleStatusToggle(member, groupId)
                }
                val btnEdit = TextView(requireContext()).apply {
                    text = "Edit"
                    setTextColor(ContextCompat.getColor(context, R.color.purple))
                    setPadding(8, 4, 8, 4)
                }


                btnEdit.setOnClickListener {
                    showRoleChangeDialog(member, groupId)
                }
                btnEdit.visibility =
                    if (canManageGroup) View.VISIBLE else View.GONE

                btnAction.visibility =
                    if (canManageGroup) View.VISIBLE else View.GONE

                val newRow = TableRow(activity).apply {
                    addView(tvNo)
                    addView(tvMemberName)
                    addView(tvJoiningDate)
                    addView(tvRole)
                    addView(tvStatus)
                    addView(btnAction)
                    addView(btnEdit)
                }

                tableLayoutGroupMemberDetails = binding.tableGroupDetails
                tableLayoutGroupMemberDetails.addView(newRow)
            }
        }
    }

    private fun handleStatusToggle(member: GroupMembers, groupId: Int) {

        if (member.role == "PRIMARY_MODERATOR") {
            showToast("Cannot modify primary moderator")
            return
        }

        val newStatus = if (member.status == "ACTIVE") "INACTIVE" else "ACTIVE"

        val updatedMember = member.copy(
            status = newStatus,
            updatedBy = SessionManager.userId
        )
        lifecycleScope.launch {
            groupViewModel.updateGroupMember(
                updatedMember
            )
            if (member.userId == SessionManager.userId) {
                SessionManager.updateGroupMembership(updatedMember.toUserGroupMembership())
            }
            fetchAllMembers(groupId)
        }
    }

    private fun cleanTable(table: TableLayout) {
        while (table.childCount > 1) {
            table.removeViewAt(1)
        }
    }

    private fun showRoleChangeDialog(member: GroupMembers, groupId: Int) {

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
                handleRoleUpdate(member, groupId, newRole)
            }
            .show()
    }

    private fun handleRoleUpdate(member: GroupMembers, groupId: Int, newRole: String) {

        // permission check
        if (newRole == "MODERATOR" && !SessionManager.isPrimaryGroupModerator(groupId)) {
            showToast("Only primary moderator can assign moderators")
            return
        }

        val updatedMember = member.copy(
            role = newRole,
            updatedBy = SessionManager.userId
        )
        lifecycleScope.launch {
            groupViewModel.updateGroupMember(
                updatedMember
            )
            if (member.userId == SessionManager.userId) {
                SessionManager.updateGroupMembership(updatedMember.toUserGroupMembership())
            }
            fetchAllMembers(groupId)
        }
    }

    private fun showAddGroupMemberDialog(context: Context, groupId: Int) {
        val dialogBinding = DialogAddGroupMemberBinding.inflate(LayoutInflater.from(context))
        dialog = BottomSheetDialog(context)
        dialog?.setContentView(dialogBinding.root)
        dialog?.show()

        var userId = 0
        dialogBinding.buttonSaveGrpMember.isEnabled = false

        lifecycleScope.launch {

            val isAdmin = SessionManager.isAdmin() // or however you determine role
            val currentUserId = SessionManager.userId
            val users = userViewModel.fetchUsers(isAdmin, currentUserId).first()

            val userNames = users.map { "${it.firstName} ${it.lastName}" }

            val adapter = ArrayAdapter(
                context,
                R.layout.dropdown_item_apnabank,
                userNames
            )
            dialogBinding.editTextAutoMember.setAdapter(adapter)
            dialogBinding.editTextAutoMember.threshold = 0

            dialogBinding.editTextAutoMember.setOnItemClickListener { parent, _, position, _ ->
                val selectedName = parent.getItemAtPosition(position) as String
                val selectedUser = users.find { "${it.firstName} ${it.lastName}" == selectedName }
                userId = selectedUser?.userId ?: 0
                dialogBinding.buttonSaveGrpMember.isEnabled = userId != 0
            }

            dialogBinding.editTextAutoMember.addTextChangedListener {
                if (it.isNullOrEmpty()) {
                    dialogBinding.editTextAutoMember.showDropDown()
                }
                dialogBinding.buttonSaveGrpMember.isEnabled = false
                userId = 0
            }

            dialogBinding.editTextAutoMember.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    dialogBinding.editTextAutoMember.showDropDown()
                }
            }
        }

        dialogBinding.buttonSaveGrpMember.setOnClickListener {
            if (userId != 0) {
                lifecycleScope.launch {
                    val isMemberAlreadyAdded =
                        groupViewModel.checkIfGroupMemberAlreadyAdded(userId, groupId)
                    if (!isMemberAlreadyAdded) {
                        val isModerator = dialogBinding.checkboxModerator.isChecked

                        val role = if (isModerator) "MODERATOR" else "MEMBER"
                        runCatching {
                            groupViewModel.createGroupMember(userId, groupId, role, SessionManager.userId)
                        }.onSuccess {
                            dialog?.dismiss()
                            fetchAllMembers(groupId)
                        }.onFailure {
                            Toast.makeText(
                                context,
                                getString(R.string.error_failed_add_member, it.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(context,
                            getString(R.string.error_member_already_added), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context,
                    getString(R.string.message_select_valid_member), Toast.LENGTH_SHORT).show()
            }
        }
        dialogBinding.buttonCancelGrpMember.setOnClickListener {
            dialog?.dismiss()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
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

    override fun onDestroyView() {
        super.onDestroyView()

        fetchMembersJob?.cancel()
        dialog?.dismiss()
        dialog = null

        cleanTable(binding.tableGroupDetails)
        binding.textViewGroupNameValue.text = ""
        binding.textViewModeratorValue.text = ""

        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.show()
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
