// RegisterFragment.kt
package com.mynikatech.apnafund.ui.auth

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.data.model.Groups
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.databinding.FragmentRegisterBinding
import com.mynikatech.apnafund.net.ApiException
import com.mynikatech.apnafund.net.dto.ModeratorRegistrationResponse
import com.mynikatech.apnafund.net.dto.RegisterModeratorRequest
import com.mynikatech.apnafund.net.dto.SaveOrUpdateUserResponse
import com.mynikatech.apnafund.net.dto.UserSaveSource
import com.mynikatech.apnafund.net.dto.UserState
import com.mynikatech.apnafund.net.dto.UserStatusResponse
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.ApnaBankDate
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.Converters.toTitleCase
import com.mynikatech.apnafund.util.GroupInputValidator
import com.mynikatech.apnafund.util.UserInputValidator
import com.mynikatech.apnafund.util.assessPasswordStrength
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var auth: FirebaseAuth
    private val userViewModel: UserViewModel by viewModels()
    private var currentUserStatus: UserStatusResponse? = null
    private var lastHandledEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear() // clear any existing toolbar icons
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
        }, viewLifecycleOwner)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        binding.buttonRegister.isEnabled = false
        setupForm()
        // Phone input filter: only digits, max 10, not starting with 0
        binding.editTextPhone.filters = arrayOf(
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
        binding.editTextFirstName.addTextChangedListener { validateUserInput() }
        binding.editTextEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val email = binding.editTextEmail.text.toString().trim()
                if (email.isNotEmpty()) {
                    userViewModel.fetchUserByEmail(email)

                }
            }
        }

        userViewModel.userLiveData.observe(viewLifecycleOwner) { user ->

            val email = binding.editTextEmail.text.toString().trim()

            if (email.isBlank() || email == lastHandledEmail) return@observe

            lastHandledEmail = email

            if (user == null) {

                val phone = binding.editTextPhone.text.toString().trim()

                // Only check phone if entered fully
                if (phone.length == 10) {

                    lifecycleScope.launch {
                        val currentPhone = binding.editTextPhone.text.toString().trim()
                        if (currentPhone != phone) return@launch
                        val phoneResponse = try {
                            userViewModel.getUserByPhone(phone)
                        } catch (e: ApiException) {
                            if (e.code != 404) {
                                showToast(getString(R.string.error_server))
                                return@launch
                            }
                            null
                        }

                        val phoneUser = phoneResponse?.user

                        if (phoneUser != null) {
                            showToast(getString(R.string.error_phone_number_registered_login))
                            disableAllFields()
                            return@launch
                        }

                        // Both email & phone are new
                        showNewUserForm()
                        enableAllFields()
                    }

                } else {
                    // Phone not entered yet → allow typing
                    showNewUserForm()
                    enableAllFields()
                }

                return@observe
            }

            val state = deriveUserState(user)
            handleUserState(state, user)
        }
        binding.editTextEmail.addTextChangedListener {
            lastHandledEmail = null
            resetFormState()
            validateUserInput()
        }
        binding.editTextPhone.addTextChangedListener { text ->

            val phone = text.toString().trim()

            resetFormState()
            validateUserInput()

            if (phone.length == 10) {
                checkPhoneExists(phone)
            }
        }
        binding.editTextGroupName.addTextChangedListener { validateUserInput() }
        binding.editTextPassword.addTextChangedListener { validateUserInput() }
        binding.editTextConfirmPassword.addTextChangedListener { validateUserInput() }
        val radioGroup = binding.radioGroupRole
        radioGroup.setOnCheckedChangeListener { _, _ ->
            applyRoleState()
            validateUserInput()
        }
        binding.iconModeratorInfo.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.text_moderator_role))
                .setMessage(getString(R.string.text_moderator_info))
                .setPositiveButton(getString(R.string.text_button_ok), null)
                .show()
        }
        binding.buttonRegister.setOnClickListener {
            binding.buttonRegister.isEnabled = false
            binding.buttonRegister.text = getString(R.string.text_registering)
            val firstName = binding.editTextFirstName.text.toString().trim().toTitleCase()
            val lastName = binding.editTextLastName.text.toString().trim().toTitleCase()
            val email = binding.editTextEmail.text.toString()
            val phone = binding.editTextPhone.text.toString()
            val password = binding.editTextPassword.text.toString()
            val isModerator = binding.radioModerator.isChecked
            val groupName = binding.editTextGroupName.text.toString().trim().toTitleCase()
            val groupDesc = binding.editTextGroupDesc.text.toString()
            val user = userViewModel.userLiveData.value
            val state = deriveUserState(user)

            when (state) {

                UserState.NEW -> {
                    // allowed to proceed
                }

                UserState.ACTIVE -> {
                    showExistingUserDialog()
                    resetRegisterButton()
                    return@setOnClickListener
                }

                UserState.INACTIVE -> {
                    showInactiveUserDialog(user!!)
                    resetRegisterButton()
                    return@setOnClickListener
                }

                UserState.INVITED_NOT_VERIFIED -> {
                    showInvitedVerifyDialog(user!!)
                    resetRegisterButton()
                    return@setOnClickListener
                }

                UserState.INVITED_VERIFIED_NO_PASSWORD -> {
                    showInvitedVerifiedSetPasswordDialog(user!!)
                    resetRegisterButton()
                    return@setOnClickListener
                }
            }
            if (!validatePasswordInputs()) {
                binding.buttonRegister.isEnabled = true
                binding.buttonRegister.text = getString(R.string.button_register)
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                val phoneResponse = try {
                    userViewModel.getUserByPhone(phone)
                } catch (e: ApiException) {
                    if (e.code != 404) {
                        showToast(getString(R.string.error_server))
                        resetRegisterButton()
                        return@launch
                    }
                    null
                }

                if (phoneResponse?.user != null) {
                    showToast(getString(R.string.error_phone_number_registered_login))
                    resetRegisterButton()
                    return@launch
                }
                if (isModerator) {
                    saveModeratorUser(
                        "1", firstName, lastName,
                        email, phone, groupName, groupDesc, password
                    ).onSuccess { resp ->
                        showToast(getString(R.string.message_submit_approval))
                        if (!resp.emailVerified) {

                            val action =
                                RegisterFragmentDirections
                                    .actionRegisterFragmentToVerifyEmailFragment(
                                        userId = resp.userId,
                                        email = email,
                                        userName = "$firstName $lastName",
                                        shouldSetPin = binding.setPinFlag.isChecked,
                                        emailOtpExpiresAtMillis = resp.emailOtpExpiresAtMillis ?: 0,
                                        purpose = ApnaBankConstants.TEXT_EMAIL_VERIFY
                                    )
                            findNavController().navigate(action)
                        } else {
                            // DEV fallback only (email verification disabled)

                            if (binding.setPinFlag.isChecked) {
                                val action =
                                    RegisterFragmentDirections
                                        .actionRegisterFragmentToSetPinFragment(resp.userId)
                                findNavController().navigate(action)
                            } else {
                                findNavController()
                                    .navigate(R.id.action_registerFragment_to_loginFragment)
                            }
                        }
                    }
                        .onFailure {
                            binding.buttonRegister.isEnabled = true
                            binding.buttonRegister.text = getString(R.string.button_register)
                            showToast(
                                getString(R.string.error_server)
                            )
                        }
                } else {

                    if (state != UserState.NEW) {
                        resetRegisterButton()
                        return@launch
                    }

                    // BUSINESS RULE CHECK If not moderator message to contact admin to get the invite.
                    if (!isModerator) {
                        showToast(getString(R.string.message_contact_moderator_add_before_registering))
                        resetRegisterButton()
                        return@launch
                    }
                }
            }
        }
        binding.buttonCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun applyRoleState() {
        val isModerator = binding.radioModerator.isChecked

        binding.moderatorFieldsContainer.visibility =
            if (isModerator) View.VISIBLE else View.GONE

        binding.editTextGroupName.isEnabled = isModerator
        binding.editTextGroupDesc.isEnabled = isModerator
    }

    private fun resetRegisterButton() {
        binding.buttonRegister.isEnabled = true
        binding.buttonRegister.text = getString(R.string.button_register)
    }

    private fun checkPhoneExists(phone: String) {

        lifecycleScope.launch {

            val response = try {
                userViewModel.getUserByPhone(phone)
            } catch (e: ApiException) {
                if (e.code != 404) {
                    showToast(getString(R.string.error_server))
                }
                return@launch
            }

            val user = response?.user ?: return@launch

            val state = deriveUserState(user)

            when (state) {

                UserState.ACTIVE -> {
                    showExistingUserDialog()
                }

                UserState.INACTIVE -> {
                    showInactiveUserDialog(user)
                }

                UserState.INVITED_NOT_VERIFIED -> {
                    showInvitedVerifyDialog(user)
                }

                UserState.INVITED_VERIFIED_NO_PASSWORD -> {
                    showInvitedVerifiedSetPasswordDialog(user)
                }

                else -> {
                    // NEW → do nothing
                }
            }
        }
    }

    private fun deriveUserState(user: UsersDto?): UserState {
        return when {
            user == null -> UserState.NEW

            user.status == ApnaBankConstants.STATUS_INACTIVE ->
                UserState.INACTIVE

            user.isInvited && !user.emailVerified ->
                UserState.INVITED_NOT_VERIFIED

            user.isInvited && user.emailVerified && user.passwordHash.isNullOrBlank() ->
                UserState.INVITED_VERIFIED_NO_PASSWORD

            !user.passwordHash.isNullOrBlank() && user.emailVerified ->
                UserState.ACTIVE

            else -> UserState.NEW
        }
    }

    private fun handleUserState(state: UserState, user: UsersDto?) {

        when (state) {

            UserState.NEW -> {
                showNewUserForm()
                enableAllFields()
            }

            UserState.INVITED_NOT_VERIFIED -> {
                showInvitedVerifyDialog(user!!)
            }

            UserState.INVITED_VERIFIED_NO_PASSWORD -> {
                showInvitedVerifiedSetPasswordDialog(user!!)
            }

            UserState.ACTIVE -> {
                showExistingUserDialog()
            }

            UserState.INACTIVE -> {
                showInactiveUserDialog(user!!)
            }
        }
    }

    private fun showInvitedVerifyDialog(user: UsersDto) {

        // Optional: lock rest of UI
        disableAllFields()

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_account_exists))
            .setMessage(
                "An account has already been created for you by " +
                        "${user.createdByName ?: "your moderator"}.\n\n" +
                        "Please verify your email to activate your account."
            )
            .setCancelable(false)

            .setPositiveButton(getString(R.string.label_continue)) { _, _ ->
                navigateToVerifyEmail(user)
            }

            .setNegativeButton(getString(R.string.text_cancel_button)) { _, _ ->
                enableAllFields()   // allow editing again
                validateUserInput()
                lastHandledEmail = null
            }

            .show()
    }

    private fun showInvitedVerifiedSetPasswordDialog(user: UsersDto) {

        // Optional: lock rest of UI
        disableAllFields()
        val invitedBy = user.createdByName ?: getString(R.string.text_your_moderator)

        val message = getString(
            R.string.dialog_invited_verified_set_password_message,
            invitedBy
        )
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_account_exists))
            .setMessage(message)
            .setCancelable(false)

            .setPositiveButton(getString(R.string.label_continue)) { _, _ ->
                navigateToSetPassword(user)
            }

            .setNegativeButton(getString(R.string.text_cancel_button)) { _, _ ->
                enableAllFields()   // allow editing again
                validateUserInput()
                lastHandledEmail = null
            }

            .show()
    }

    private fun navigateToVerifyEmail(user: UsersDto) {

        val action = RegisterFragmentDirections
            .actionRegisterFragmentToVerifyEmailFragment(
                userId = user.userId!!,
                email = user.emailId,
                userName = "${user.firstName} ${user.lastName}",
                shouldSetPin = binding.setPinFlag.isChecked,
                emailOtpExpiresAtMillis = 0L,
                purpose = ApnaBankConstants.TEXT_INVITE_VERIFY
            )

        findNavController().navigate(action)
    }

    private fun navigateToSetPassword(user: UsersDto) {

        val action = RegisterFragmentDirections
            .actionRegisterFragmentToSetPasswordFragment(
                userId = user.userId!!,
            )

        findNavController().navigate(action)
    }

    private fun showInactiveUserDialog(user: UsersDto) {
        disableAllFields()

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_account_inactive))
            .setMessage(getString(R.string.dialog_account_inactive_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.msg_go_to_login)) { _, _ ->
                navigateToLogin()
            }

            .setNegativeButton(getString(R.string.text_cancel_button)) { _, _ ->
                // Allow user to edit email/phone again
                enableAllFields()
                validateUserInput()
                lastHandledEmail = null
            }
            .show()
    }

    private fun disableAllFields() {

        // Keep email editable so user can correct it
        binding.editTextEmail.isEnabled = true

        // Disable personal fields
        binding.editTextFirstName.isEnabled = false
        binding.editTextLastName.isEnabled = false
        binding.editTextPhone.isEnabled = false

        // Disable password fields
        binding.editTextPassword.isEnabled = false
        binding.editTextConfirmPassword.isEnabled = false

        // Disable role selection
        binding.radioMember.isEnabled = false
        binding.radioModerator.isEnabled = false
        binding.radioGroupRole.isEnabled = false

        // Disable moderator-specific fields
        binding.editTextGroupName.isEnabled = false
        binding.editTextGroupDesc.isEnabled = false

        // Optional: clear validation errors (prevents stale red errors)
        binding.editTextFirstName.error = null
        binding.editTextLastName.error = null
        binding.editTextEmail.error = null
        binding.editTextPhone.error = null
        binding.editTextPassword.error = null
        binding.editTextConfirmPassword.error = null
        binding.editTextGroupName.error = null
    }

    private fun enableAllFields() {

        // Text fields
        binding.editTextFirstName.isEnabled = true
        binding.editTextLastName.isEnabled = true
        binding.editTextEmail.isEnabled = true
        binding.editTextPhone.isEnabled = true
        binding.editTextPassword.isEnabled = true
        binding.editTextConfirmPassword.isEnabled = true

        // Role selection
        binding.radioMember.isEnabled = true
        binding.radioModerator.isEnabled = true
        binding.radioGroupRole.isEnabled = true
        binding.editTextGroupName.isEnabled = true
        binding.editTextGroupDesc.isEnabled = true
        binding.textSetPasswordInfo.visibility = View.GONE

        // Button state should depend on validation (NOT force enabled)
        validateUserInput()
        applyRoleState()
    }

    fun showNewUserForm() {
        resetFormState()
        binding.editTextFirstName.visibility = View.VISIBLE
        binding.editTextLastName.visibility = View.VISIBLE
        binding.editTextPhone.visibility = View.VISIBLE
        binding.editTextPassword.visibility = View.VISIBLE
        binding.editTextConfirmPassword.visibility = View.VISIBLE
        binding.buttonRegister.text = getString(R.string.button_register)
    }

    fun showSetPasswordFlow(user: UsersDto) {
        binding.textSetPasswordInfo.visibility = View.VISIBLE
        binding.textSetPasswordInfo.text =
            getString(R.string.message_invited_user_set_password)
        binding.editTextFirstName.isEnabled = false
        binding.editTextLastName.isEnabled = false
        binding.editTextPhone.isEnabled = false
        binding.radioGroupRole.isEnabled = false
        binding.editTextFirstName.setText(user.firstName)
        binding.editTextLastName.setText(user.lastName)
        binding.editTextPhone.setText(user.phoneNumber)
        for (i in 0 until binding.radioGroupRole.childCount) {
            binding.radioGroupRole.getChildAt(i).isEnabled = false
        }
        binding.editTextPassword.visibility = View.VISIBLE
        binding.editTextConfirmPassword.visibility = View.VISIBLE
        binding.buttonRegister.text = getString(R.string.button_set_password)
    }

    fun showLoginRedirect() {
        Log.d("Register Fragment: ShowLoginRedirect", "triggered")
        Toast.makeText(
            requireContext(),
            getString(R.string.error_account_already_exists),
            Toast.LENGTH_LONG
        ).show()
        binding.buttonRegister.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    fun resetFormState() {
        binding.editTextFirstName.isEnabled = true
        binding.editTextLastName.isEnabled = true
        binding.editTextPhone.isEnabled = true
        binding.textSetPasswordInfo.visibility = View.GONE
        for (i in 0 until binding.radioGroupRole.childCount) {
            binding.radioGroupRole.getChildAt(i).isEnabled = true
        }
        applyRoleState()
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

    fun setupForm() {

        // Required fields
        listOf(
            binding.inputLayoutFirstName,
            binding.inputLayoutEmail,
            binding.inputLayoutPhone,
            binding.inputLayoutGroupName

        ).forEach { it.markRequired() }

        // Info dialogs
        val infoFields = mapOf(
            binding.inputLayoutEmail to Pair(
                R.string.title_user_email_info,
                R.string.info_user_email
            ),
            binding.inputLayoutPhone to Pair(
                R.string.title_user_phone_info,
                R.string.info_user_phone
            ),
            binding.inputLayoutGroupName to Pair(
                R.string.title_user_group_info,
                R.string.info_user_group
            )
        )

        infoFields.forEach { (view, data) ->
            view.setInfoDialog(data.first, data.second)
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private suspend fun checkPreAddedMember(email: String, phone: String): Boolean {
        return userViewModel.isDuplicate(email, phone)
    }

    private suspend fun saveRegularUser(
        uid: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        password: String
    ): Result<SaveOrUpdateUserResponse> {

        val passwordHash = Converters.hashPassword(password)
        val userCode = Converters.generateUserCode(firstName, lastName)
        val now = System.currentTimeMillis()

        val user = Users(
            userId = 0,
            firstName = firstName,
            lastName = lastName,
            emailId = email,
            phoneNumber = phone,
            firebaseUserId = uid,
            userCode = userCode,
            passwordHash = passwordHash,
            createdByUserId = null,
            userSaveSource = UserSaveSource.SELF_REGISTER,
            createdAt = now,
            updatedByUserId = null,
            updatedAt = null,
            createdByName = null
        )

        return userViewModel.saveOrUpdateUser(
            user = user,
            userSaveSource = UserSaveSource.SELF_REGISTER
        )
    }

    private suspend fun saveModeratorUser(
        uid: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        groupName: String,
        desc: String,
        password: String
    ): Result<ModeratorRegistrationResponse> {

        val passwordHash = Converters.hashPassword(password)
        val userCode = Converters.generateUserCode(firstName, lastName)
        val now = System.currentTimeMillis()

        val user = Users(
            userId = 0,
            firstName = firstName,
            lastName = lastName,
            emailId = email,
            phoneNumber = phone,
            firebaseUserId = uid,
            passwordHash = passwordHash,
            userCode = userCode,
            createdByUserId = null,
            userSaveSource = UserSaveSource.SELF_REGISTER,
            createdAt = now,
            updatedByUserId = null,
            updatedAt = null,
            createdByName = null
        )
        val group = Groups(
            groupName = groupName,
            status = ApnaBankConstants.STATUS_PENDING,
            createdDate = ApnaBankDate.getCurrentDate(),
            description = desc,
            groupCode = Converters.generateGroupCode(groupName)
        )
        val regModReq = RegisterModeratorRequest(
            user = user.toDto(),
            group = group.toDto(),
            requestorId = SessionManager.userId
        )
        return userViewModel.registerModeratorAndGroup(regModReq)
    }


    // Local function to validate inputs
    private fun validateUserInput() {
        val firstName = binding.editTextFirstName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim().lowercase()
        val phone = binding.editTextPhone.text.toString().trim()
        val groupName = binding.editTextGroupName.text.toString().trim()
        val isModerator = binding.radioModerator.isChecked

        var isGroupNameValid = true

        val isFirstNameValid = UserInputValidator.isFirstNameValid(firstName)
        val isEmailValid = UserInputValidator.isEmailValid(email)
        val isPhoneValid = UserInputValidator.isPhoneValid(phone)
        if (isModerator)
            isGroupNameValid = GroupInputValidator.isGroupNameValidInput(groupName)
        val isPasswordValid = validatePasswordInputs()

        binding.buttonRegister.isEnabled =
            isFirstNameValid && isEmailValid && isPhoneValid && isGroupNameValid && isPasswordValid

        if (!isFirstNameValid && firstName.isNotEmpty()) {
            binding.editTextFirstName.error = ApnaBankConstants.FIRST_NAME_ERROR_MESSAGE
        }
        if (!isGroupNameValid && groupName.isNotEmpty()) {
            binding.editTextGroupName.error = ApnaBankConstants.GROUP_NAME_ERROR_MESSAGE
        }

        if (!isEmailValid && email.isNotEmpty()) {
            binding.editTextEmail.error = ApnaBankConstants.INVALID_EMAIL_ERROR_MESSAGE
        }

        if (!isPhoneValid && phone.isNotEmpty()) {
            binding.editTextPhone.error = ApnaBankConstants.INVALID_PHONE_ERROR_MESSAGE
        }
    }

    private fun validatePasswordInputs(): Boolean {
        val password = binding.editTextPassword.text.toString()
        val confirmPassword = binding.editTextConfirmPassword.text.toString()

        // Basic length check
        if (password.isNotEmpty() && password.length < 8) {
            binding.editTextPassword.error = getString(R.string.error_pwd_length)
            return false
        }
        // Password strength
        val strength = assessPasswordStrength(password)
        if (password.isNotEmpty() && strength.name == ApnaBankConstants.TEXT_WEAK) {
            binding.editTextPassword.error =
                getString(R.string.error_password_weak)
            return false
        }
        // Confirm password match
        if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
            binding.editTextConfirmPassword.error = getString(R.string.error_pwd_not_match)
            return false
        }
        // Clear errors if valid
        binding.editTextPassword.error = null
        binding.editTextConfirmPassword.error = null
        return true
    }

    override fun onPause() {
        super.onPause()
        userViewModel.userLiveData.removeObservers(viewLifecycleOwner)
    }

    private fun showExistingUserDialog() {

        // Lock the form so user can’t continue registering
        disableAllFields()

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_account_exists))
            .setMessage(getString(R.string.dialog_account_exists_login_message))
            .setCancelable(false)

            .setPositiveButton(getString(R.string.msg_go_to_login)) { _, _ ->
                navigateToLogin()
            }

            .setNegativeButton(getString(R.string.text_cancel_button)) { _, _ ->
                // Allow user to edit email/phone again
                enableAllFields()
                validateUserInput()
                lastHandledEmail = null
            }

            .show()
    }

    private fun navigateToLogin() {
        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
    }
}

