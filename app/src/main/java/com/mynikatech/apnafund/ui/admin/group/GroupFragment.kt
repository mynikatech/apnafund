package com.mynikatech.apnafund.ui.admin.group

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
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
import com.google.android.material.textfield.TextInputLayout
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.mappers.toEntity
import com.mynikatech.apnafund.data.model.Groups
import com.mynikatech.apnafund.databinding.DialogAddGroupBinding
import com.mynikatech.apnafund.databinding.FragmentGroupBinding
import com.mynikatech.apnafund.net.dto.GroupsWithModeratorDto
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.viewmodel.GroupViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.ApnaBankDate
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.Converters.toTitleCase
import com.mynikatech.apnafund.util.GroupInputValidator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GroupFragment : Fragment() {

    private lateinit var binding: FragmentGroupBinding

    private lateinit var tableLayoutGroupDetails: TableLayout

    private val userViewModel: UserViewModel by viewModels()

    private val groupViewModel: GroupViewModel by viewModels()

    private lateinit var whiteBg: Drawable

    private lateinit var grayBg: Drawable

    private val isAdmin = SessionManager.isAdmin()


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
        binding.layoutEmpty.textEmptyMessage.text =
            getString(R.string.text_no_groups_available)
        fetchAllGroups()

    }

    private fun updateUI(isEmpty: Boolean) {

        binding.layoutEmpty.root.visibility =
            if (isEmpty) View.VISIBLE else View.GONE

        binding.scrollviewHeader.visibility =
            if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun fetchAllGroups() {
        lifecycleScope.launch {
            try {
                if (isAdmin) {
                    groupViewModel.fetchAllGroupsWithModerator()
                        .collectLatest { groups ->
                            populateUserTable(groups)
                        }
                } else {
                    userViewModel.getGroupsForModeratorUserWithModInfo(SessionManager.userId)
                        .collectLatest { groups ->
                            populateUserTable(groups)
                        }
                }
            } catch (e: Exception) {
                Log.e("GroupFragment", "Error fetching groups", e)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_failed_load_groups), Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun populateUserTable(groups: List<GroupsWithModeratorDto>) {
        updateUI(groups.isEmpty())
        cleanTable(tableLayoutGroupDetails)
        if (groups.isEmpty()) return
        lifecycleScope.launch {
            try {


                for (i in groups.indices) {
                    try {
                        val group = groups[i]
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
                        val moderatorName = group.moderatorName ?: "-"
                        val tvModerator = createTableCell(moderatorName, moderatorName)
                        val tvCreatedDate =
                            createTableCell(group.createdDate, gravity = Gravity.END)
                        val tvDescription =
                            createTableCell(text = group.description, tooltip = group.description)
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
                    } catch (rowException: Exception) {
                        Log.e("GroupFragment", "Error rendering row $i", rowException)
                    }
                }
            } catch (e: Exception) {
                Log.e("GroupFragment", "Error populating table", e)
                Toast.makeText(requireContext(), "Error loading data", Toast.LENGTH_SHORT).show()
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
        existingGroup: GroupsWithModeratorDto? = null
    ) {
        val dialogBinding = DialogAddGroupBinding.inflate(LayoutInflater.from(context))
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(dialogBinding.root)
        dialog.show()
        dialogBinding.setupForm()
        var userId = 0
        dialogBinding.buttonSaveGrp.text =
            if (addOrEditFlag == 1) getString(R.string.text_update_group) else getString(R.string.text_add_group)

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
                dialogBinding.editTextGroupName.error = ApnaBankConstants.GROUP_NAME_ERROR_MESSAGE
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
                Log.e("GroupFragment", "Error loading users", e)
                Toast.makeText(
                    context,
                    getString(R.string.error_failed_load_users), Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialogBinding.buttonSaveGrp.setOnClickListener {
            dialogBinding.buttonSaveGrp.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {


                    val groupName = dialogBinding.editTextGroupName.text.toString().trim()
                        .toTitleCase()
                    val description = dialogBinding.editTextGroupDesc.text
                        .toString()
                        .trim()
                    val status = if (isAdmin) {
                        ApnaBankConstants.STATUS_ACTIVE
                    } else {
                        ApnaBankConstants.STATUS_PENDING
                    }
                    val existingGroupWithoutMod = existingGroup?.toEntity()
                    val groupToSave = existingGroupWithoutMod?.copy(
                        groupName = groupName,
                        moderator = userId,
                        status = existingGroupWithoutMod.status,
                        description = description
                    ) ?: Groups(
                        groupName = groupName,
                        moderator = userId,
                        status = status,
                        createdDate = ApnaBankDate.getCurrentDate(),
                        description = description,
                        groupCode = Converters.generateGroupCode(groupName)
                    )
                    groupViewModel.saveOrUpdateGroup(groupToSave)

                    val message = if (isAdmin) {
                        getString(R.string.message_group_created_success)
                    } else {
                        getString(R.string.message_group_request_submitted_for_approval)

                    }

                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    fetchAllGroups()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Log.e("GroupFragment", "Error saving group", e)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_failed_save_group), Toast.LENGTH_LONG
                    )
                        .show()
                    dialogBinding.buttonSaveGrp.isEnabled = true
                }
            }
        }
        dialogBinding.buttonCancelGrp.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun showConfirmToggleGroupStatus(group: GroupsWithModeratorDto) {

        val action =
            if (group.status == ApnaBankConstants.STATUS_ACTIVE)
                getString(R.string.action_deactivate) else getString(R.string.action_deactivate)
        val title = getString(R.string.group_label, action)
        val message = getString(
            R.string.message_group_action_confirmation,
            action,
            group.groupName
        )
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.text_yes)) { _, _ ->
                toggleGroupStatus(group)
            }
            .setNegativeButton(getString(R.string.text_no), null)
            .show()
    }

    private fun toggleGroupStatus(group: GroupsWithModeratorDto) {
        lifecycleScope.launch {
            val updatedStatus: String = if (group.status == ApnaBankConstants.STATUS_ACTIVE) {
                ApnaBankConstants.STATUS_INACTIVE
            } else {
                ApnaBankConstants.STATUS_ACTIVE
            }
            val updatedGroup = group.copy(status = updatedStatus)
            groupViewModel.saveOrUpdateGroup(updatedGroup.toEntity())
            val statusText = if (updatedGroup.status == ApnaBankConstants.STATUS_ACTIVE) {
                getString(R.string.status_activated)
            } else {
                getString(R.string.status_deactivated)
            }
            val message = getString(
                R.string.message_group_status,
                updatedGroup.groupName,
                statusText
            )

            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

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