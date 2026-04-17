package com.mynikatech.apnafund.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.mappers.toEntity
import com.mynikatech.apnafund.databinding.FragmentForgotPasswordBinding
import com.mynikatech.apnafund.net.ApiException
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import android.util.Log

class ForgotPasswordFragment : Fragment() {

    private lateinit var binding: FragmentForgotPasswordBinding
    private val userViewModel: UserViewModel by viewModels()
    private lateinit var userEmail: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        arguments?.let {
            userEmail = it.getString(ApnaBankConstants.USER_EMAIL) ?: ""
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.editTextEmail.setText(userEmail)

        binding.buttonSendResetCode.setOnClickListener {
            binding.buttonSendResetCode.isEnabled = false
            val email = binding.editTextEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(),
                    getString(R.string.message_enter_email_id), Toast.LENGTH_SHORT).show()
                binding.buttonSendResetCode.isEnabled = true
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(),
                    getString(R.string.warn_enter_valid_email), Toast.LENGTH_SHORT)
                    .show()
                binding.buttonSendResetCode.isEnabled = true
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val loginResponse = userViewModel.getUserByEmail(email)

                if (loginResponse == null) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_no_user_found_email),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.buttonSendResetCode.isEnabled = true
                    return@launch
                }
                val user = loginResponse.user.toEntity()
                val resp = try {
                    userViewModel.resendEmailVerification(
                        userId = user.userId,
                        email = email,
                        userName = "${user.firstName} ${user.lastName}",
                        purpose = ApnaBankConstants.RESET_PASSWORD
                    )
                } catch (e: ApiException) {
                    Log.e("ForgotPasswordFragment", e.message.toString())
                    showToast(getString(R.string.error_server))
                    binding.buttonSendResetCode.isEnabled = true
                    return@launch
                }
                findNavController().navigate(
                    ForgotPasswordFragmentDirections
                        .actionForgotPasswordFragmentToVerifyEmailFragment(
                            userId = user.userId,
                            email = email,
                            userName = "${user.firstName} ${user.lastName}",
                            emailOtpExpiresAtMillis = resp.emailOtpExpiresAtMillis,
                            purpose = ApnaBankConstants.RESET_PASSWORD
                        )
                )
            }
        }
        binding.authToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().navigate(
                ForgotPasswordFragmentDirections.actionForgotPasswordFragmentToLoginFragment()
            )
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
