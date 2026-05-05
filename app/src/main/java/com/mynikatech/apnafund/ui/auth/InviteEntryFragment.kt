package com.mynikatech.apnafund.ui.auth

import android.app.AlertDialog
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.databinding.FragmentInviteEntryBinding
import com.mynikatech.apnafund.net.dto.UserState
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel


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

        setupListeners()
    }

    private fun setupListeners() {

        binding.buttonContinue.setOnClickListener {
            binding.buttonContinue.isEnabled = false
            binding.buttonCancel.isEnabled = false
            val email = binding.etEmail.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = getString(R.string.wran_enter_email_to_continue)
                binding.buttonContinue.isEnabled = true
                binding.buttonCancel.isEnabled = true
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                binding.etEmail.error = getString(R.string.warn_enter_valid_email)
                binding.buttonContinue.isEnabled = true
                binding.buttonCancel.isEnabled = true
                return@setOnClickListener
            }
            handleInvitePreview(email)


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

                UserState.INVITED_VERIFIED_NO_PASSWORD -> {
                    // Already verified → go to set password
                    showAlreadyVerifiedDialog(user)
                }

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
                binding.etEmail.requestFocus()
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
                binding.etEmail.text?.clear()
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

            .setPositiveButton(getString(R.string.label_continue)) { _, _ ->

                val action = InviteEntryFragmentDirections
                    .actionInviteEntryFragmentToVerifyEmailFragment(
                        userId = user.userId!!,
                        email = user.emailId,
                        userName = "${user.firstName} ${user.lastName}",
                        shouldSetPin = false,
                        emailOtpExpiresAtMillis = 0L,
                        purpose = ApnaBankConstants.TEXT_INVITE_VERIFY
                    )

                findNavController().navigate(action)
            }

            .setNegativeButton(getString(R.string.text_cancel_button)) { _, _ ->
                // Stay on screen, allow editing email
                binding.etEmail.requestFocus()
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
                binding.etEmail.text?.clear()
                binding.buttonContinue.isEnabled = true
                binding.buttonCancel.isEnabled = true
            }

            .show()
    }

    private fun handleInvitePreview(email: String) {

        binding.buttonContinue.isEnabled = false

        userViewModel.fetchUserByEmail(email)
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
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

            user.isInvited && !user.emailVerified ->
                UserState.INVITED_NOT_VERIFIED

            user.isInvited && user.emailVerified && user.passwordHash.isNullOrBlank() ->
                UserState.INVITED_VERIFIED_NO_PASSWORD

            !user.passwordHash.isNullOrBlank() && user.emailVerified ->
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


}