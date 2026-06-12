package com.mynikatech.apnafund.ui.auth

import android.app.AlertDialog
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.databinding.FragmentInviteEntryBinding
import com.mynikatech.apnafund.net.ApiException
import com.mynikatech.apnafund.net.dto.Channel
import com.mynikatech.apnafund.net.dto.UserState
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch


class InviteEntryFragment : Fragment() {

    private var _binding: FragmentInviteEntryBinding? = null
    private val binding get() = _binding!!
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInviteEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(
                        R.id.action_inviteEntryFragment_to_loginFragment
                    )
                }
            }
        )
        binding.inviteToolbar.title = getString(R.string.header_join_via_invite)
        listOf(
            binding.etPhone,
            binding.etInviteCode
        ).forEach { it.markRequired() }

        val infoFields = mapOf(
            binding.etEmail to Pair(
                R.string.title_user_email_info,
                R.string.info_user_email
            ),
            binding.etPhone to Pair(
                R.string.title_user_phone_info,
                R.string.info_user_phone
            ),
            binding.etInviteCode to Pair(
                R.string.title_invite_code_info,
                R.string.info_invite_code
            )
        )

        infoFields.forEach { (view, data) ->
            view.setInfoDialog(data.first, data.second)
        }

        setupListeners()
    }

    private fun setupListeners() {

        binding.buttonContinue.setOnClickListener {
            binding.buttonContinue.isEnabled = false
            binding.buttonCancel.isEnabled = false
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val inviteCode = binding.etInviteCode.text.toString().trim()

            if (email.isNotBlank() && !isValidEmail(email)) {
                binding.etEmail.error = getString(R.string.warn_enter_valid_email)
                binding.buttonContinue.isEnabled = true
                binding.buttonCancel.isEnabled = true
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                binding.etPhone.error = getString(R.string.error_phone_number_required)
                binding.buttonContinue.isEnabled = true
                binding.buttonCancel.isEnabled = true
                return@setOnClickListener
            }

            if (!isValidPhone(phone)) {
                binding.etPhone.error = getString(R.string.error_enter_10_digit_phone_number)
                binding.buttonContinue.isEnabled = true
                binding.buttonCancel.isEnabled = true
                return@setOnClickListener
            }
            if (inviteCode.isBlank()) {
                binding.etInviteCode.error =
                    getString(R.string.error_invite_code_required)

                binding.buttonContinue.isEnabled = true
                binding.buttonCancel.isEnabled = true
                return@setOnClickListener
            }
            handleInvitePreview(email, phone, inviteCode)


        }
        userViewModel.userLiveData.observe(viewLifecycleOwner) { user ->

            binding.buttonContinue.isEnabled = true

            if (user == null) {
                showNoInviteDialog()
                binding.buttonCancel.isEnabled = true
                return@observe
            }

            val state = deriveUserState(user)

            when (state) {

                UserState.INVITED_NOT_VERIFIED -> {
                    // Valid invite → continue flow
                    proceedWithInvite(user)
                }

                /*UserState.INVITED_VERIFIED_NO_PASSWORD -> {
                    // Already verified → go to set password
                    showAlreadyVerifiedDialog(user)
                }*/

                UserState.ACTIVE -> {
                    // Your requirement
                    showActiveUserDialog()
                }

                UserState.INACTIVE -> {
                    showInactiveUserDialog(user)
                }

                else -> {
                    showNoInviteDialog()
                }
            }
        }
        binding.buttonCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        userViewModel.errorLiveData.observe(viewLifecycleOwner) { error ->

            when (error.code) {

                429 -> {
                    showRateLimitDialog()
                }

                else -> {
                    showToast(getString(R.string.error_server))
                }
            }
        }


    }

    private fun showNoInviteDialog() {

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_no_invite_found))
            .setMessage(getString(R.string.dialog_no_invite_found_message))
            .setCancelable(false)

            .setPositiveButton(getString(R.string.button_register)) { _, _ ->
                findNavController().navigate(
                    R.id.action_inviteEntryFragment_to_registerFragment
                )
            }

            .setNegativeButton(getString(R.string.text_cancel_button)) { _, _ ->
                binding.etPhone.text?.clear()
                binding.etInviteCode.text?.clear()
                binding.etEmail.text?.clear()

                binding.etPhone.requestFocus()

                binding.buttonContinue.isEnabled = true
                binding.buttonCancel.isEnabled = true
            }

            .show()
    }

    private fun showInactiveUserDialog(user: UsersDto) {

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_account_inactive))
            .setMessage(getString(R.string.dialog_account_inactive_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.msg_go_to_login)) { _, _ ->
                findNavController().navigate(
                    R.id.action_inviteEntryFragment_to_loginFragment
                )
            }
            .setNegativeButton(getString(R.string.text_cancel_button)) { _, _ ->
                binding.buttonContinue.isEnabled = true
                binding.buttonCancel.isEnabled = true
            }
            .show()
    }

    private fun showAlreadyVerifiedDialog(user: UsersDto) {

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_account_already_verified))
            .setMessage(getString(R.string.dialog_account_verified_set_password_message))
            .setPositiveButton(getString(R.string.label_continue)) { _, _ ->
                findNavController().navigate(
                    InviteEntryFragmentDirections
                        .actionInviteEntryFragmentToSetPasswordFragment(user.userId!!)
                )
            }
            .setNegativeButton(getString(R.string.text_cancel_button), null)
            .show()
    }

    private fun proceedWithInvite(user: UsersDto) {

        val invitedBy = user.createdByName ?: getString(R.string.text_moderator)

        val message = getString(
            R.string.dialog_invite_message,
            invitedBy
        )

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_invite_found))
            .setMessage(message)
            .setCancelable(false)

            .setPositiveButton(
                getString(R.string.label_continue)
            ) { _, _ ->

                lifecycleScope.launch {

                    try {

                        val resp =
                            userViewModel.sendOtp(
                                userId = user.userId!!,
                                phone = user.phoneNumber!!,
                                purpose = ApnaBankConstants.INVITE_VERIFY
                            )

                        val action =
                            InviteEntryFragmentDirections
                                .actionInviteEntryFragmentToVerifyOtpFragment(
                                    userId = user.userId!!,
                                    email = user.emailId,
                                    phoneNumber = user.phoneNumber,
                                    userName =
                                        "${user.firstName} ${user.lastName}",
                                    purpose =
                                        ApnaBankConstants.INVITE_VERIFY,
                                    channel =
                                        Channel.WHATSAPP.name,
                                    otpExpiresAtMillis =
                                        resp.otpExpiresAtMillis,
                                    shouldSetPin = false
                                )

                        findNavController().navigate(action)

                    } catch (e: ApiException) {

                        showToast(
                            getString(
                                R.string.message_unable_to_send_code
                            )
                        )

                        binding.buttonContinue.isEnabled = true
                        binding.buttonCancel.isEnabled = true
                    }
                }
            }

            .setNegativeButton(getString(R.string.text_cancel_button)) { _, _ ->
                // Stay on screen, allow editing phone
                binding.etPhone.requestFocus()
                binding.buttonContinue.isEnabled = true
                binding.buttonCancel.isEnabled = true
            }

            .show()
    }

    private fun showActiveUserDialog() {

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_account_exists))
            .setMessage(getString(R.string.dialog_account_exists_login_message))
            .setCancelable(false)

            .setPositiveButton(getString(R.string.msg_go_to_login)) { _, _ ->
                findNavController().navigate(
                    R.id.action_inviteEntryFragment_to_loginFragment
                )
            }

            .setNegativeButton(getString(R.string.text_cancel_button)) { _, _ ->
                // Stay on screen, allow user to edit email
                binding.etPhone.text?.clear()
                binding.etInviteCode.text?.clear()
                binding.etEmail.text?.clear()

                binding.etPhone.requestFocus()

                binding.buttonContinue.isEnabled = true
                binding.buttonCancel.isEnabled = true
            }

            .show()
    }

    private fun handleInvitePreview(email: String?, phone: String, inviteCode: String) {

        binding.buttonContinue.isEnabled = false
        userViewModel.fetchUserByPhoneAndGroupCode(phone, inviteCode)
        //userViewModel.fetchUserByEmailandPhone(email, phone)
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.length == 10 && phone.all { it.isDigit() }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun deriveUserState(user: UsersDto?): UserState {
        return when {
            user == null -> UserState.NEW

            user.status == ApnaBankConstants.STATUS_INACTIVE ->
                UserState.INACTIVE

            user.isInvited && user.passwordHash.isNullOrBlank() ->
                UserState.INVITED_NOT_VERIFIED

            user.isInvited && !user.phoneVerified ->
                UserState.INVITED_NOT_VERIFIED

            user.isInvited && user.phoneVerified && user.passwordHash.isNullOrBlank() ->
                UserState.INVITED_VERIFIED_NO_PASSWORD

            !user.passwordHash.isNullOrBlank() && user.phoneVerified ->
                UserState.ACTIVE

            else -> UserState.NEW
        }
    }

    private fun showRateLimitDialog() {

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_too_many_attempts))
            .setMessage(getString(R.string.dialog_rate_limit_message))
            .setPositiveButton(getString(R.string.text_button_ok), null)
            .show()
    }

    fun EditText.markRequired() {

        val currentHint = hint?.toString() ?: return

        val spannable = SpannableString("$currentHint *")

        spannable.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    context,
                    com.google.android.material.R.color.design_default_color_error
                )
            ),
            spannable.length - 1,
            spannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        hint = spannable
    }

    fun EditText.setInfoDialog(
        titleRes: Int,
        messageRes: Int
    ) {

        setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd = 2

                compoundDrawables[drawableEnd]?.let {

                    if (event.rawX >= (right - it.bounds.width())) {

                        AlertDialog.Builder(context)
                            .setTitle(context.getString(titleRes))
                            .setMessage(context.getString(messageRes))
                            .setPositiveButton(
                                context.getString(R.string.text_button_ok),
                                null
                            )
                            .show()

                        return@setOnTouchListener true
                    }
                }
            }

            false
        }
    }


}