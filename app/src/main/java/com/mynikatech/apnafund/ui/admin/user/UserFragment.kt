package com.mynikatech.apnafund.ui.admin.user

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.mynikatech.apnafund.BuildConfig
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.UserWithGroup
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.databinding.DialogAddUserBinding
import com.mynikatech.apnafund.databinding.FragmentUserBinding
import com.mynikatech.apnafund.net.HttpClientProvider
import com.mynikatech.apnafund.net.dto.LoginUserResponse
import com.mynikatech.apnafund.net.dto.UserSaveSource
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

    private val userViewModel: UserViewModel by viewModels()

    private val groupViewModel: GroupViewModel by viewModels()

    private lateinit var adapter: UserAdapter

    val isAdmin = SessionManager.isAdmin()
    val moderatorGroupId = SessionManager.groupId ?: 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserBinding.inflate(inflater, container, false)

        if (SessionManager.canManageUsers())
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
        adapter = UserAdapter(
            onEdit = { user ->
                showAddUserDialog(requireContext(), 1, user)
            },
            onToggle = { user ->
                showConfirmToggleUserStatus(user)
            }
        )

        binding.recyclerUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerUsers.adapter = adapter
        observeUsers()
    }

    private fun observeUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Now collect Room and render
                userViewModel.usersFlow(isAdmin, moderatorGroupId)
                    .collectLatest { usersWithGroup -> adapter.updateList(usersWithGroup) }
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

    fun DialogAddUserBinding.setupForm() {

        // Required fields
        listOf(
            inputLayoutFirstName,
            inputLayoutEmail,
            inputLayoutPhone,
            textInputGroup

        ).forEach { it.markRequired() }

        // Info dialogs
        val infoFields = mapOf(
            inputLayoutEmail to Pair(
                R.string.title_user_email_info,
                R.string.info_user_email
            ),
            inputLayoutPhone to Pair(
                R.string.title_user_phone_info,
                R.string.info_user_phone
            ),
            textInputGroup to Pair(
                R.string.title_user_creation_group_info,
                R.string.info_user_creation_group
            )
        )

        infoFields.forEach { (view, data) ->
            view.setInfoDialog(data.first, data.second)
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

                userViewModel.saveOrUpdateUser(
                    updatedUser,
                    userSaveSource = UserSaveSource.ADMIN_UPDATE
                )

                Toast.makeText(
                    requireContext(),
                    "${ApnaBankConstants.USER_TEXT} ${updatedUser.firstName} ${updatedUser.lastName ?: ""} ${
                        if (updatedUser.status == ApnaBankConstants.STATUS_ACTIVE)
                            ApnaBankConstants.ACTIVATED_TEXT else ApnaBankConstants.DEACTIVATED_TEXT
                    }",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_user_not_found),
                    Toast.LENGTH_SHORT
                ).show()
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
        dialogBinding.setupForm()
        var detectedExistingUser: Users? = null
        val groupMap = mutableMapOf<String, Int>()
        fun checkExistingUser(
            email: String? = null,
            phone: String? = null
        ) {

            lifecycleScope.launch {

                val existingResponse =
                    when {
                        !email.isNullOrEmpty() ->
                            userViewModel.findExistingUserByEmail(email)

                        !phone.isNullOrEmpty() ->
                            userViewModel.findExistingUserByPhone(phone)

                        else -> null
                    }

                val selectedGroup =
                    dialogBinding.editTextGroup.text.toString()

                val selectedGroupId =
                    groupMap[selectedGroup]

                val result = determineExistingUserState(
                    response = existingResponse,
                    selectedGroupId = selectedGroupId,
                    currentUserId = existingUser?.userId ?: 0
                )

                val isModerator =
                    dialogBinding.checkboxModerator.isChecked

                val groupRole =
                    if (isModerator) {
                        "MODERATOR"
                    } else {
                        "MEMBER"
                    }

                handleExistingUserDetected(
                    result = result,
                    dialogBinding = dialogBinding,
                    dialog = dialog,
                    selectedGroupId = selectedGroupId,
                    groupRole = groupRole
                )
            }
        }
        dialogBinding.editTextEmail.setOnFocusChangeListener { _, hasFocus ->

            if (!hasFocus) {

                val email =
                    dialogBinding.editTextEmail.text
                        .toString()
                        .trim()

                if (email.isNotEmpty()) {

                    checkExistingUser(email = email)
                }
            }
        }

        dialogBinding.editTextPhone.setOnFocusChangeListener { _, hasFocus ->

            if (!hasFocus) {

                val phone =
                    dialogBinding.editTextPhone.text
                        .toString()
                        .trim()

                if (phone.length == 10) {

                    checkExistingUser(phone = phone)
                }
            }
        }
        if (isAdmin) {
            lifecycleScope.launch {
                groupViewModel.fetchAllGroups().collectLatest { groupList ->
                    val groupNames = mutableListOf(getString(R.string.text_select_group))
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
            if (addOrEditFlag == 1) getString(R.string.text_update_user) else getString(R.string.text_add_user)

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
            dialogBinding.buttonSaveUser.isEnabled = false
            dialogBinding.buttonSaveUser.text = getString(R.string.button_saving_progress)
            val email = dialogBinding.editTextEmail.text.toString().trim()
            val phone = dialogBinding.editTextPhone.text.toString().trim()
            val userId = existingUser?.userId ?: 0
            val group = dialogBinding.editTextGroup.text.toString()
            val firstName = dialogBinding.editTextFirstName.text.toString().trim()
                .toTitleCase()
            val lastName = dialogBinding.editTextLastName.text.toString().trim().toTitleCase()
            val selectedGroupId = groupMap[group]
            lifecycleScope.launch {
                try {
                    val isModerator = dialogBinding.checkboxModerator.isChecked

                    val groupRole = if (isModerator) {
                        "MODERATOR"
                    } else {
                        "MEMBER"
                    }
                    val existingResponse = when {

                        email.isNotEmpty() ->
                            userViewModel.findExistingUserByEmail(
                                email
                            )

                        phone.length == 10 ->
                            userViewModel.findExistingUserByPhone(
                                phone
                            )

                        else -> null
                    }

                    val existingCheckResult =
                        determineExistingUserState(
                            response = existingResponse,
                            selectedGroupId = selectedGroupId,
                            currentUserId = userId
                        )
                    when (existingCheckResult.state) {

                        ExistingUserState.ALREADY_IN_GROUP -> {

                            Toast.makeText(
                                context,
                                getString(
                                    R.string.error_user_already_in_group
                                ),
                                Toast.LENGTH_LONG
                            ).show()

                            dialogBinding.buttonSaveUser.isEnabled = true

                            dialogBinding.buttonSaveUser.text =
                                getString(
                                    R.string.text_save_button
                                )

                            return@launch
                        }

                        ExistingUserState.EXISTS_IN_OTHER_GROUP -> {

                            val response =
                                existingCheckResult.response
                                    ?: return@launch

                            showExistingUserGroupAdditionDialog(
                                response = response,
                                dialog = dialog,
                                selectedGroupId = selectedGroupId,
                                groupRole = groupRole
                            )

                            dialogBinding.buttonSaveUser.isEnabled = true

                            dialogBinding.buttonSaveUser.text =
                                getString(
                                    R.string.text_save_button
                                )

                            return@launch
                        }

                        ExistingUserState.NEW_USER -> {
                            // continue normal flow
                        }
                    }
                    val existingUserFull =
                        if (userId != 0) userViewModel.fetchUser(userId) else null
                    // if moderator selecting group is mandatory
                    if (!isAdmin && selectedGroupId == null) {
                        Toast.makeText(
                            context,
                            getString(R.string.error_valid_group_selection), Toast.LENGTH_LONG
                        ).show()
                        dialogBinding.buttonSaveUser.isEnabled = true
                        dialogBinding.buttonSaveUser.text =
                            getString(R.string.button_saving_progress)
                        return@launch
                    }
                    val userSaveSource = if (!isAdmin) {
                        UserSaveSource.MODERATOR_CREATE
                    } else {
                        UserSaveSource.ADMIN_CREATE
                    }
                    val now = System.currentTimeMillis()
                    val userToSave = Users(
                        userId = userId,
                        firstName = firstName,
                        lastName = lastName,
                        emailId = email,
                        phoneNumber = phone,
                        status = existingUserFull?.status ?: ApnaBankConstants.STATUS_ACTIVE,
                        isPinSet = existingUserFull?.isPinSet == true,
                        passwordHash = existingUserFull?.passwordHash,
                        firebaseUserId = existingUserFull?.firebaseUserId,
                        hashPIN = existingUserFull?.hashPIN,
                        createdDate = existingUserFull?.createdDate
                            ?: ApnaBankDate.getCurrentDate(),
                        userCode = existingUserFull?.userCode ?: Converters.generateUserCode(
                            firstName,
                            lastName
                        ),
                        isInvited = true,
                        createdByUserId = SessionManager.userId,
                        userSaveSource = userSaveSource,
                        createdAt = now,
                        updatedByUserId = null,
                        updatedAt = null,
                        createdByName = null
                    )
                    val result = userViewModel.saveOrUpdateUser(
                        userToSave,
                        groupId = selectedGroupId ?: 0,
                        userSaveSource,
                        groupRole
                    )
                    if (result.isSuccess) {

                        val response = result.getOrNull()

                        Toast.makeText(
                            context,
                            "User saved successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        dialog.dismiss()
                        userViewModel.refreshTrigger.value = Unit

                    } else {

                        val error = result.exceptionOrNull()

                        Toast.makeText(
                            context,
                            error?.message ?: getString(R.string.error_saving_user),
                            Toast.LENGTH_LONG
                        ).show()
                        dialogBinding.buttonSaveUser.isEnabled = true
                        dialogBinding.buttonSaveUser.text = getString(R.string.text_save_button)
                    }
                    dialog.dismiss()
                    userViewModel.refreshTrigger.value = Unit
                } catch (e: Exception) {
                    Log.e("UserDialog", "Error saving user", e)

                    Toast.makeText(
                        context,
                        getString(R.string.error_saving_user),
                        Toast.LENGTH_LONG
                    ).show()
                    dialogBinding.buttonSaveUser.isEnabled = true
                    dialogBinding.buttonSaveUser.text = getString(R.string.text_save_button)
                }
            }
        }

        dialogBinding.buttonCancelUser.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun showExistingUserGroupAdditionDialog(
        response: LoginUserResponse,
        dialog: BottomSheetDialog,
        selectedGroupId: Int?,
        groupRole: String
    ) {

        val existingUser = response.user ?: return

        val groupNames = response.groups
            .joinToString { it.groupName }

        AlertDialog.Builder(requireContext())
            .setTitle(
                getString(
                    R.string.title_existing_user_found
                )
            )
            .setMessage(
                getString(
                    R.string.message_existing_user_found_groups,
                    groupNames
                )
            )
            .setPositiveButton(
                getString(R.string.label_continue)
            ) { _, _ ->

                lifecycleScope.launch {

                    try {

                        groupViewModel.createGroupMember(
                            memberId = existingUser.userId!!,
                            groupId = selectedGroupId ?: 0,
                            role = groupRole,
                            requestorId = SessionManager.userId
                        )

                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.msg_user_added_group_success
                            ),
                            Toast.LENGTH_LONG
                        ).show()

                        dialog.dismiss()

                        userViewModel.refreshTrigger.value = Unit

                    } catch (e: Exception) {

                        Log.e(
                            "UserDialog",
                            "Error adding existing user to group",
                            e
                        )

                        Toast.makeText(
                            requireContext(),
                            e.message
                                ?: getString(
                                    R.string.error_adding_user_to_group
                                ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(
                getString(R.string.text_cancel_button),
                null
            )
            .show()
    }

    private fun determineExistingUserState(
        response: LoginUserResponse?,
        selectedGroupId: Int?,
        currentUserId: Int = 0
    ): ExistingUserCheckResult {

        val existingUser = response?.user

        // CASE 1
        if (existingUser == null) {
            return ExistingUserCheckResult(
                ExistingUserState.NEW_USER
            )
        }

        // Ignore self during edit
        if (
            currentUserId != 0 &&
            existingUser.userId == currentUserId
        ) {
            return ExistingUserCheckResult(
                ExistingUserState.NEW_USER
            )
        }

        val alreadyInGroup = response.groups.any {
            it.groupId == selectedGroupId
        }

        // CASE 2
        if (alreadyInGroup) {

            return ExistingUserCheckResult(
                ExistingUserState.ALREADY_IN_GROUP,
                response
            )
        }

        // CASE 3
        return ExistingUserCheckResult(
            ExistingUserState.EXISTS_IN_OTHER_GROUP,
            response
        )
    }

    private fun handleExistingUserDetected(
        result: ExistingUserCheckResult,
        dialogBinding: DialogAddUserBinding,
        dialog: BottomSheetDialog,
        selectedGroupId: Int?,
        groupRole: String
    ) {

        when (result.state) {

            ExistingUserState.NEW_USER -> {
                // do nothing
            }

            ExistingUserState.ALREADY_IN_GROUP -> {

                AlertDialog.Builder(requireContext())
                    .setTitle(
                        getString(
                            R.string.title_existing_user_found
                        )
                    )
                    .setMessage(
                        getString(
                            R.string.error_user_already_in_group
                        )
                    )
                    .setPositiveButton(
                        getString(R.string.text_button_ok),
                        null
                    )
                    .show()
            }

            ExistingUserState.EXISTS_IN_OTHER_GROUP -> {

                val response = result.response ?: return

                showExistingUserGroupAdditionDialog(
                    response = response,
                    dialog = dialog,
                    selectedGroupId = selectedGroupId,
                    groupRole = groupRole
                )
            }

        }
    }


}