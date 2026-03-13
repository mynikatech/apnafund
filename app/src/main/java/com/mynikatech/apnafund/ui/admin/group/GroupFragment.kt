package com.mynikatech.apnafund.ui.admin.group

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.mappers.toEntity
import com.mynikatech.apnafund.data.model.Groups
import com.mynikatech.apnafund.databinding.DialogAddGroupBinding
import com.mynikatech.apnafund.databinding.FragmentGroupBinding
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.viewmodel.GroupViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.ApnaBankDate
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.Converters.toTitleCase
import com.mynikatech.apnafund.util.GroupInputValidator
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class GroupFragment : Fragment() {

    private lateinit var binding: FragmentGroupBinding

    private lateinit var tableLayoutGroupDetails: TableLayout

    private val userViewModel: UserViewModel by viewModels()

    private val groupViewModel: GroupViewModel by viewModels()

    private lateinit var whiteBg: Drawable

    private lateinit var grayBg: Drawable

    private val isAdmin = SessionManager.isAdmin()
    private val moderatorGroupId = SessionManager.groupId ?: 0

    private val userGroups = SessionManager.userGroups ?: emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentGroupBinding.inflate(inflater, container, false)
        val context = activity
        binding.fabAddGroup.visibility =
            if (Converters.userHasPrivilege(ApnaBankConstants.ADD_GROUP_PRIV)) View.VISIBLE
            else View.GONE
        binding.fabAddGroup.setOnClickListener {
            if (context != null)
                showAddGroupDialog(context, 0)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        tableLayoutGroupDetails = binding.tableGroupDetails
        whiteBg =
            ContextCompat.getDrawable(requireContext(), R.drawable.table_cell_border_white)!!
        grayBg =
            ContextCompat.getDrawable(requireContext(), R.drawable.table_cell_border_gray)!!
        fetchAllGroups()
    }

    private fun fetchAllGroups() {

        lifecycleScope.launch {
            if (isAdmin) {
                groupViewModel.fetchAllGroups()
                    .collectLatest { groups ->
                        populateUserTable(groups ?: emptyList())
                    }
            } else {
                userViewModel.getGroupsForModeratorUser(SessionManager.userId)
                    .collectLatest { groups ->
                        populateUserTable(groups.toEntity() ?: emptyList())
                    }
            }
        }
    }

    private fun populateUserTable(groups: List<Groups>) {
        cleanTable(tableLayoutGroupDetails)
        lifecycleScope.launch {
            val userDeferredList = groups.map { group ->
                async {
                    userViewModel.fetchUser(group.moderator!!)
                }
            }

            val userList = userDeferredList.map { it.await() }
            for (i in groups.indices) {
                val group = groups[i]
                val user = userList[i]
                val serialNum = (1 + i).toString()
                val tvNo = createTableCell(serialNum, gravity = Gravity.CENTER)
                val tvGroupName = createTableCell(text = group.groupName)
                tvGroupName.apply {
                    setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.link_colour
                        )
                    ) // Optional: make it look clickable
                    isClickable = true
                    setOnClickListener {
                        val navController = requireActivity()
                            .supportFragmentManager
                            .findFragmentById(R.id.nav_host_fragment)  // replace with your actual host ID
                            ?.findNavController()
                        navController?.navigate(
                            GroupFragmentDirections.actionGroupFragmentToGroupDetailsFragment(
                                group.groupId
                            )
                        )
                    }
                }
                val userName = "${user?.firstName} ${user?.lastName}"
                val tvModerator = createTableCell(userName, userName)
                val tvCreatedDate =
                    createTableCell(group.createdDate, gravity = Gravity.END)
                val tvDescription = createTableCell(text = group.description, tooltip = group.description)
                val btnEdit = createIconButton(R.drawable.icon_edit) {
                    showAddGroupDialog(requireContext(), 1, groups[i])
                }
                // If the user has the add/edit group privilege
                if (Converters.userHasPrivilege(ApnaBankConstants.ADD_GROUP_PRIV))
                    btnEdit.visibility = View.VISIBLE
                else
                    btnEdit.visibility = View.GONE
                val status = groups[i].status
                val drawable = when (status) {
                    ApnaBankConstants.STATUS_ACTIVE -> R.drawable.ic_block
                    ApnaBankConstants.STATUS_INACTIVE -> R.drawable.ic_check_circle
                    ApnaBankConstants.STATUS_PENDING -> R.drawable.ic_pending // or ic_pending
                    else -> R.drawable.ic_block
                }
                val btnToggleActive = createIconButton(drawable, status) {
                    showConfirmToggleGroupStatus(groups[i])
                }
                // Disable button when pending
                if (status == ApnaBankConstants.STATUS_PENDING) {
                    btnToggleActive.isEnabled = false
                    btnToggleActive.alpha = 0.5f
                }

                val newRow = TableRow(activity).apply {
                    layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
                    addView(tvNo, 0)
                    addView(tvGroupName, 1)
                    addView(tvModerator, 2)
                    addView(tvCreatedDate, 3)
                    addView(tvDescription, 4)
                    addView(btnEdit, 5)
                    addView(btnToggleActive, 6)
                    background = if (i % 2 == 0) whiteBg else grayBg
                }
                tableLayoutGroupDetails.addView(newRow)
            }
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
        var userId = 0
        dialogBinding.buttonSaveGrp.text = if (addOrEditFlag == 1) "Update Group" else "Add Group"

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
                dialogBinding.editTextGroupName.error = "Group name must be more than 3 characters"
            }
        }

        dialogBinding.editTextGroupName.addTextChangedListener {
            validateInputs()
        }

        // Load user list and handle selection
        // when the user is admin all the users should be made available
        // however when the user is not admin only the user should be available

        lifecycleScope.launch {
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
                        val existingModerator = users.find { it.userId == existingGroup.moderator }
                        val fullName = "${existingModerator?.firstName} ${existingModerator?.lastName}"

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
                        "Group created successfully."
                    } else {
                        "Group request submitted for approval. You will receive a notification once it is approved."
                    }

                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Failed to save group. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                fetchAllGroups()
                dialog.dismiss()
            }
        }
        dialogBinding.buttonCancelGrp.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun showConfirmToggleGroupStatus(group: Groups) {
        val action =
            if (group.status == ApnaBankConstants.STATUS_ACTIVE) "Deactivate" else "Activate"

        AlertDialog.Builder(requireContext())
            .setTitle("$action user")
            .setMessage("Are you sure you want to $action ${group.groupName}")
            .setPositiveButton("Yes") { _, _ ->
                toggleGroupStatus(group)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun toggleGroupStatus(group: Groups) {
        lifecycleScope.launch {
            val updatedStatus: String = if (group.status == ApnaBankConstants.STATUS_ACTIVE) {
                ApnaBankConstants.STATUS_INACTIVE
            } else {
                ApnaBankConstants.STATUS_ACTIVE
            }
            val updatedGroup = group.copy(status = updatedStatus)
            groupViewModel.saveOrUpdateGroup(updatedGroup)
            Toast.makeText(
                requireContext(),
                "Group ${updatedGroup.groupName} ${if (updatedGroup.status == ApnaBankConstants.STATUS_ACTIVE) "Activated" else "Deactivated"}",
                Toast.LENGTH_SHORT
            ).show()
            fetchAllGroups()
        }
    }

    private fun cleanTable(table: TableLayout) {
        val childCount = table.childCount

        // Remove all rows except the first header row
        if (childCount > 1) {
            table.removeViews(1, childCount - 1)
        }
    }

    private fun createTableCell(
        text: String?,
        tooltip: String? = null,
        gravity: Int = Gravity.START,
    ): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            this.gravity = gravity
            maxLines = 1
            if (!tooltip.isNullOrBlank()) {
                ViewCompat.setTooltipText(this, tooltip)
            }
            layoutParams = TableRow.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun createIconButton(
        drawableRes: Int, tooltip: String? = null,
        onClick: () -> Unit
    ): ImageButton {
        val sizeInPx = (19 * resources.displayMetrics.density).toInt()
        return ImageButton(requireContext()).apply {
            setImageResource(drawableRes)
            layoutParams = TableRow.LayoutParams(sizeInPx, sizeInPx)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(4, 4, 4, 4)
            background = null
            if (!tooltip.isNullOrBlank()) {
                ViewCompat.setTooltipText(this, tooltip)
            }
            setOnClickListener { onClick() }
        }
    }
}