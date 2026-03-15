// RegisterFragment.kt
package com.mynikatech.apnafund.ui.auth

import android.os.Bundle
import android.text.InputFilter
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
import com.google.firebase.auth.FirebaseAuth
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.data.model.Groups
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.databinding.FragmentRegisterBinding
import com.mynikatech.apnafund.net.dto.ModeratorRegistrationResponse
import com.mynikatech.apnafund.net.dto.RegisterModeratorRequest
import com.mynikatech.apnafund.net.dto.SaveOrUpdateUserResponse
import com.mynikatech.apnafund.net.dto.UserSaveSource
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
        binding.editTextEmail.addTextChangedListener { validateUserInput() }
        binding.editTextPhone.addTextChangedListener { validateUserInput() }
        binding.editTextGroupName.addTextChangedListener { validateUserInput() }
        binding.editTextPassword.addTextChangedListener { validateUserInput() }
        binding.editTextConfirmPassword.addTextChangedListener { validateUserInput() }
        val radioGroup = binding.radioGroupRole
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_member -> {
                    binding.moderatorFieldsContainer.visibility = View.GONE
                }

                R.id.radio_moderator -> {
                    binding.moderatorFieldsContainer.visibility = View.VISIBLE
                    validateUserInput()
                }
            }
        }
        binding.buttonRegister.setOnClickListener {
            binding.buttonRegister.isEnabled = false
            binding.buttonRegister.text = "Registering..."
            val firstName = binding.editTextFirstName.text.toString().trim().toTitleCase()
            val lastName = binding.editTextLastName.text.toString().trim().toTitleCase()

            val email = binding.editTextEmail.text.toString()
            val phone = binding.editTextPhone.text.toString()
            val password = binding.editTextPassword.text.toString()
            val isModerator = binding.radioModerator.isChecked
            val groupName = binding.editTextGroupName.text.toString().trim().toTitleCase()
            val groupDesc = binding.editTextGroupDesc.text.toString()
            lifecycleScope.launch {
                val isDuplicate = userViewModel.isDuplicate(email, phone)
                Log.d("DuplicateCheck", "isDuplicate: $isDuplicate for $email, $phone")
                if (isDuplicate) {
                    Toast.makeText(
                        context,
                        getString(R.string.text_existing_user_error_message), Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
            }
            if (!validatePasswordInputs()) return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                if (isModerator) {
                    saveModeratorUser(
                        "1", firstName, lastName,
                        email, phone, groupName, groupDesc, password
                    ).onSuccess { resp ->
                        showToast("Submitted for approval. You will receive an email once approved")
                        if (!resp.emailVerified) {

                            val action =
                                RegisterFragmentDirections
                                    .actionRegisterFragmentToVerifyEmailFragment(
                                        userId = resp.userId,
                                        email = email,
                                        userName = "$firstName $lastName",
                                        shouldSetPin = binding.setPinFlag.isChecked,
                                        emailOtpExpiresAtMillis = resp.emailOtpExpiresAtMillis ?: 0,
                                        purpose = "EMAIL_VERIFY"
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
                            showToast(
                                "Some issues with server. Please raise a support ticket or contact admin."
                            )
                        }
                } else {
                    if (checkPreAddedMember(email, phone)) {
                        val shouldSetPin = binding.setPinFlag.isChecked
                        saveRegularUser("1", firstName, lastName, email, phone, password)
                            .onSuccess { resp ->
                                if (!resp.emailVerified) {
                                    val action = RegisterFragmentDirections
                                        .actionRegisterFragmentToVerifyEmailFragment(
                                            userId = resp.userId,
                                            email = email,
                                            userName = "$firstName $lastName",
                                            shouldSetPin = shouldSetPin,
                                            emailOtpExpiresAtMillis = resp.emailOtpExpiresAtMillis
                                                ?: 0,
                                            purpose = "EMAIL_VERIFY"
                                        )
                                    findNavController().navigate(action)
                                } else {
                                    if (shouldSetPin) {
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
                                showToast(
                                    "Some issues with server. Please raise a support ticket or contact admin."
                                )
                            }
                    } else {
                        binding.buttonRegister.isEnabled = true
                        showToast("Please contact moderator to add you before registering")
                    }
                }
            }
        }
        binding.buttonCancel.setOnClickListener {
            findNavController().navigateUp()
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

        val user = Users(
            userId = 0,
            firstName = firstName,
            lastName = lastName,
            emailId = email,
            phoneNumber = phone,
            firebaseUserId = uid,
            userCode = userCode,
            passwordHash = passwordHash
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

        val user = Users(
            userId = 0,
            firstName = firstName,
            lastName = lastName,
            emailId = email,
            phoneNumber = phone,
            firebaseUserId = uid,
            passwordHash = passwordHash,
            userCode = userCode
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
            group = group.toDto()
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
            binding.editTextPassword.error = "Password must be at least 8 characters"
            return false
        }
        // Password strength
        val strength = assessPasswordStrength(password)
        if (password.isNotEmpty() && strength.name == "WEAK") {
            binding.editTextPassword.error =
                "Password is too weak. Use letters, numbers, and special characters"
            return false
        }
        // Confirm password match
        if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
            binding.editTextConfirmPassword.error = "Passwords do not match"
            return false
        }
        // Clear errors if valid
        binding.editTextPassword.error = null
        binding.editTextConfirmPassword.error = null
        return true
    }
}

