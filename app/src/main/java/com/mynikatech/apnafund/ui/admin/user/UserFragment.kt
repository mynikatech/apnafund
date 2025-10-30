package com.mynikatech.apnafund.ui.admin.user

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.TextUtils
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mynikatech.apnafund.BuildConfig
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.UserWithGroup
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.databinding.DialogAddUserBinding
import com.mynikatech.apnafund.databinding.FragmentUserBinding
import com.mynikatech.apnafund.net.HttpClientProvider
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.viewmodel.GroupViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.ApnaBankDate
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.Converters.toTitleCase
import com.mynikatech.apnafund.util.UserInputValidator
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class UserFragment : Fragment() {

    private lateinit var binding: FragmentUserBinding

    private lateinit var tableLayoutUserDetails: TableLayout

    private val userViewModel: UserViewModel by viewModels()

    private val groupViewModel: GroupViewModel by viewModels()

    private lateinit var whiteBg: Drawable

    private lateinit var grayBg: Drawable

    val isAdmin = SessionManager.isAdmin()
    val moderatorGroupId = SessionManager.groupId ?: 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserBinding.inflate(inflater, container, false)

        if (Converters.userHasPrivilege(ApnaBankConstants.ADD_USER_PRIV))
            binding.fab.visibility = View.VISIBLE
        else
            binding.fab.visibility = View.GONE
        binding.fab.setOnClickListener {
            showAddUserDialog(requireContext(), 0)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("NET", "BASE(onCreate)=${BuildConfig.SERVER_BASE_URL}")
        lifecycleScope.launch {
            runCatching {
                val txt = HttpClientProvider.client.get("/_debug/echo").bodyAsText()
                Log.i("NET", "Echo -> $txt")
            }.onFailure { e ->
                Log.e("NET", "Echo failed", e)
            }
        }
        tableLayoutUserDetails = binding.tableUserDetails
        whiteBg =
            ContextCompat.getDrawable(requireContext(), R.drawable.table_cell_border_white)!!
        grayBg =
            ContextCompat.getDrawable(requireContext(), R.drawable.table_cell_border_gray)!!

        //fetchAllUsers()
        observeUsers()
    }

    private fun observeUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Kick a server -> Room sync (don’t block UI)
                launch {
                    runCatching { userViewModel.refreshUsersAndCache() }
                        .onFailure { e -> Log.e("NET", "User refresh failed", e) }
                }
                // Now collect Room and render
                userViewModel.usersFlow(isAdmin, moderatorGroupId)
                    .collectLatest { usersWithGroup -> populateUserTable(usersWithGroup) }
            }
        }
    }


    private fun fetchAllUsers() {
        lifecycleScope.launch {
            userViewModel.fetchUsersWithGroup(isAdmin, moderatorGroupId)
                .collectLatest { usersWithGroup ->
                    populateUserTable(usersWithGroup)
                }
        }
    }

    private fun populateUserTable(users: List<UserWithGroup>) {
        cleanTable(tableLayoutUserDetails)
        val noOfUsers: Int = users.size
        for (i in 0 until noOfUsers) {
            val tvNo = TextView(activity)
            val user = users[i]
            tvNo.text = "${i.plus(1)}"
            tvNo.gravity = Gravity.CENTER
            val tvFirstName = createTableCell(user.firstName, user.firstName)
            val tvLastName = createTableCell(user.lastName, user.lastName)
            val tvEmailId = createTableCell(user.emailId, user.emailId)
            val phoneNumber = TextView(activity)
            phoneNumber.text = user.phoneNumber
            phoneNumber.gravity = Gravity.END
            val groupName = createTableCell(user.groupName, user.groupName)
            val btnEdit = createIconButton(R.drawable.icon_edit) {
                showAddUserDialog(requireContext(), 1, user)
            }
            btnEdit.visibility =
                if (Converters.userHasPrivilege(ApnaBankConstants.ADD_USER_PRIV)) View.VISIBLE else View.GONE
            val status = user.status
            // assuming a flag like this exists
            val drawable =
                if (status == ApnaBankConstants.STATUS_ACTIVE) R.drawable.ic_block else R.drawable.ic_check_circle

            val btnToggleActive = createIconButton(drawable, status) {
                showConfirmToggleUserStatus(user)
            }
            val newRow = TableRow(activity)
            newRow.layoutParams =
                TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
            newRow.addView(tvNo, 0)
            newRow.addView(tvFirstName, 1)
            newRow.addView(tvLastName, 2)
            newRow.addView(tvEmailId, 3)
            newRow.addView(phoneNumber, 4)
            newRow.addView(groupName, 5)
            newRow.addView(btnEdit, 6)
            newRow.addView(btnToggleActive, 7)
            newRow.background = if (i % 2 == 0) whiteBg else grayBg
            tableLayoutUserDetails.addView(newRow)
        }

    }

    private fun showConfirmToggleUserStatus(user: UserWithGroup) {
        val action =
            if (user.status == ApnaBankConstants.STATUS_ACTIVE) ApnaBankConstants.DEACTIVATE_TEXT else ApnaBankConstants.ACTIVATE_TEXT

        AlertDialog.Builder(requireContext())
            .setTitle("$action ${ApnaBankConstants.USER_TEXT}")
            .setMessage(
                getString(
                    R.string.text_warning_confirmation,
                    action,
                    user.firstName,
                    user.lastName
                )
            )
            .setPositiveButton(ApnaBankConstants.POSITIVE_BUTTON_TEXT) { _, _ ->
                toggleUserStatus(user)
            }
            .setNegativeButton(ApnaBankConstants.NEGATIVE_BUTTON_TEXT, null)
            .show()
    }

    private fun toggleUserStatus(user: UserWithGroup) {
        lifecycleScope.launch {
            // Fetch original user by ID to retain all sensitive fields
            val existingUser = userViewModel.fetchUser(user.userId)

            if (existingUser != null) {
                val updatedStatus = if (user.status == ApnaBankConstants.STATUS_ACTIVE) {
                    ApnaBankConstants.INACTIVE_STATUS
                } else {
                    ApnaBankConstants.STATUS_ACTIVE
                }

                val updatedUser = existingUser.copy(status = updatedStatus)

                userViewModel.saveOrUpdateUser(updatedUser)

                Toast.makeText(
                    requireContext(),
                    "${ApnaBankConstants.USER_TEXT} ${updatedUser.firstName} ${updatedUser.lastName ?: ""} ${
                        if (updatedUser.status == ApnaBankConstants.STATUS_ACTIVE)
                            ApnaBankConstants.ACTIVATED_TEXT else ApnaBankConstants.DEACTIVATED_TEXT
                    }",
                    Toast.LENGTH_SHORT
                ).show()
                observeUsers()
            } else {
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddUserDialog(
        context: Context,
        addOrEditFlag: Int,
        existingUser: UserWithGroup? = null
    ) {
        val dialogBinding = DialogAddUserBinding.inflate(LayoutInflater.from(context))
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(dialogBinding.root)
        dialog.show()
        val groupMap = mutableMapOf<String, Int>()
        if (isAdmin) {
            lifecycleScope.launch {
                groupViewModel.fetchAllGroups().collectLatest { groupList ->
                    val groupNames = mutableListOf("Select Group")
                    groupNames.addAll(groupList.map { it.groupName })
                    groupMap.clear()
                    groupMap.putAll(groupList.associate { it.groupName to it.groupId })
                    val adapter = ArrayAdapter(
                        context,
                        R.layout.dropdown_item_apnabank,
                        groupNames
                    )
                    dialogBinding.editTextGroup.setAdapter(adapter)
                    dialogBinding.editTextGroup.isEnabled = true

                    val groupNameToSet = existingUser?.groupName
                    if (!groupNameToSet.isNullOrEmpty() && groupNames.contains(groupNameToSet)) {
                        dialogBinding.editTextGroup.setText(groupNameToSet, false)
                    } else if (groupNames.isNotEmpty()) {
                        // Optional: preselect first group for new users or users without group
                        dialogBinding.editTextGroup.setText("", false)
                        dialogBinding.editTextGroup.setOnClickListener { dialogBinding.editTextGroup.showDropDown() }
                    }
                }
            }
        } else {
            // Moderator - assume single group
            lifecycleScope.launch {
                val moderatorGroup = groupViewModel.fetchGroup(moderatorGroupId)
                val groupNames = listOf(moderatorGroup.groupName)
                groupMap.clear()
                groupMap[moderatorGroup.groupName] = moderatorGroup.groupId

                val adapter = ArrayAdapter(
                    context,
                    R.layout.dropdown_item_apnabank,
                    groupNames
                )
                dialogBinding.editTextGroup.setAdapter(adapter)
                dialogBinding.editTextGroup.setText(moderatorGroup.groupName, false)
                dialogBinding.editTextGroup.isEnabled = false
                dialogBinding.editTextGroup.isFocusable = false
            }
        }

        dialogBinding.editTextGroup.inputType = InputType.TYPE_NULL
        dialogBinding.editTextGroup.keyListener = null
        // Set button text
        dialogBinding.buttonSaveUser.text =
            if (addOrEditFlag == 1) "Update User" else "Add User"

        // Pre-fill fields if editing
        existingUser?.let {
            dialogBinding.editTextFirstName.setText(it.firstName)
            dialogBinding.editTextLastName.setText(it.lastName)
            dialogBinding.editTextEmail.setText(it.emailId)
            dialogBinding.editTextPhone.setText(it.phoneNumber)
        }

        dialogBinding.buttonSaveUser.isEnabled = false

        // Phone input filter: only digits, max 10, not starting with 0
        dialogBinding.editTextPhone.filters = arrayOf(
            InputFilter.LengthFilter(10),
            InputFilter { source, start, end, dest, dstart, dend ->
                val result = dest.toString().substring(0, dstart) +
                        source.subSequence(start, end) +
                        dest.toString().substring(dend)
                if (!result.matches(Regex("^\\d{0,10}$"))) return@InputFilter ""
                if (result.startsWith("0")) return@InputFilter ""
                null
            }
        )

        // Local function to validate inputs
        fun validateUserInput() {
            val firstName = dialogBinding.editTextFirstName.text.toString().trim()
            val email = dialogBinding.editTextEmail.text.toString().trim().lowercase()
            val phone = dialogBinding.editTextPhone.text.toString().trim()


            val isFirstNameValid = UserInputValidator.isFirstNameValid(firstName)
            val isEmailValid = UserInputValidator.isEmailValid(email)
            val isPhoneValid = UserInputValidator.isPhoneValid(phone)

            dialogBinding.buttonSaveUser.isEnabled =
                isFirstNameValid && isEmailValid && isPhoneValid

            if (!isFirstNameValid && firstName.isNotEmpty()) {
                dialogBinding.editTextFirstName.error =
                    ApnaBankConstants.FIRST_NAME_ERROR_MESSAGE
            }

            if (!isEmailValid && email.isNotEmpty()) {
                dialogBinding.editTextEmail.error =
                    ApnaBankConstants.INVALID_EMAIL_ERROR_MESSAGE
            }

            if (!isPhoneValid && phone.isNotEmpty()) {
                dialogBinding.editTextPhone.error =
                    ApnaBankConstants.INVALID_PHONE_ERROR_MESSAGE
            }

        }
        // Attach validation listeners
        dialogBinding.editTextFirstName.addTextChangedListener { validateUserInput() }
        dialogBinding.editTextEmail.addTextChangedListener { validateUserInput() }
        dialogBinding.editTextPhone.addTextChangedListener { validateUserInput() }
        dialogBinding.editTextGroup.setOnItemClickListener { _, _, _, _ -> validateUserInput() }

        // Save or update user
        dialogBinding.buttonSaveUser.setOnClickListener {
            val email = dialogBinding.editTextEmail.text.toString().trim()
            val phone = dialogBinding.editTextPhone.text.toString().trim()
            val userId = existingUser?.userId ?: 0
            val group = dialogBinding.editTextGroup.text.toString()
            val firstName = dialogBinding.editTextFirstName.text.toString().trim()
                .toTitleCase()
            val lastName = dialogBinding.editTextLastName.text.toString().trim().toTitleCase()
            val selectedGroupId = groupMap[group]
            lifecycleScope.launch {
                val isDuplicate = userViewModel.isDuplicate(email, phone, userId)
                val existingUserFull = if (userId != 0) userViewModel.fetchUser(userId) else null
                if (isDuplicate) {
                    Toast.makeText(
                        context,
                        getString(R.string.text_existing_user_error_message), Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                // if moderator selecting group is mandatory
                if (!isAdmin && selectedGroupId == null) {
                    Toast.makeText(
                        context,
                        "Valid group selection is required", Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                val userToSave = Users(
                    userId = userId,
                    firstName = firstName,
                    lastName = lastName,
                    emailId = email,
                    phoneNumber = phone,
                    status = existingUserFull?.status ?: ApnaBankConstants.STATUS_ACTIVE,
                    isPinSet = existingUserFull?.isPinSet ?: false,
                    passwordHash = existingUserFull?.passwordHash,
                    firebaseUserId = existingUserFull?.firebaseUserId,
                    hashPIN = existingUserFull?.hashPIN,
                    createdDate = existingUserFull?.createdDate ?: ApnaBankDate.getCurrentDate(),
                    userCode = existingUserFull?.userCode ?: Converters.generateUserCode(
                        firstName,
                        lastName
                    )
                )
                userViewModel.saveOrUpdateUser(userToSave, groupId = selectedGroupId ?: 0)
                dialog.dismiss()
                fetchAllUsers()
            }
        }

        dialogBinding.buttonCancelUser.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun createTableCell(
        text: String?,
        tooltip: String?,
        gravity: Int = Gravity.START
    ): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            this.gravity = gravity
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = (resources.displayMetrics.widthPixels * 0.25).toInt()
            ViewCompat.setTooltipText(this, tooltip)
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
            background = null
            ViewCompat.setTooltipText(this, tooltip)
            setPadding(2, 2, 2, 2)
            setOnClickListener { onClick() }
        }
    }

    private fun cleanTable(table: TableLayout) {
        val childCount = table.childCount
        // Remove all rows except the first header row
        if (childCount > 1) {
            table.removeViews(1, childCount - 1)
        }
    }
}