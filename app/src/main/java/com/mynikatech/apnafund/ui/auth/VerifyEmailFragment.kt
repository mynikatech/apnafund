package com.mynikatech.apnafund.ui.auth

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.databinding.FragmentVerifyEmailBinding
import com.mynikatech.apnafund.net.ApiException
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import android.util.Log

class VerifyEmailFragment : Fragment(R.layout.fragment_verify_email) {

    private lateinit var binding: FragmentVerifyEmailBinding
    private val userViewModel: UserViewModel by viewModels()
    private val args: VerifyEmailFragmentArgs by navArgs()

    private var userId: Int = 0
    private lateinit var email: String
    private lateinit var userName: String
    private lateinit var purpose: String
    private var shouldSetPin: Boolean = false
    private var emailOtpExpiresAt = 0L
    private var otpTimer: CountDownTimer? = null
    private var resendTimer: CountDownTimer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentVerifyEmailBinding.bind(view)

        userId = args.userId
        email = args.email
        userName = args.userName
        shouldSetPin = args.shouldSetPin
        emailOtpExpiresAt = args.emailOtpExpiresAtMillis
        purpose = args.purpose

        binding.textEmail.text = email

        binding.buttonVerify.setOnClickListener {
            binding.buttonVerify.isEnabled = false
            val otp = binding.editTextOtp.text.toString().trim()

            if (otp.length != 6) {
                Toast.makeText(requireContext(),
                    getString(R.string.error_enter_valid_6_digit_code), Toast.LENGTH_SHORT)
                    .show()
                binding.buttonVerify.isEnabled = true
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // Will throw ApiException on 400 / 500
                    userViewModel.verifyEmailOtp(otp, userId, purpose)

                    // SUCCESS
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.successful_email_verification_message),
                        Toast.LENGTH_SHORT
                    ).show()

                    when (purpose) {
                        "EMAIL_VERIFY" -> {
                            if (shouldSetPin) {
                                val action =
                                    VerifyEmailFragmentDirections
                                        .actionVerifyEmailFragmentToSetPinFragment(userId)
                                findNavController().navigate(action)
                            } else {
                                val action =
                                    VerifyEmailFragmentDirections
                                        .actionVerifyEmailFragmentToLoginFragment()
                                findNavController().navigate(action)
                            }
                        }
                        "INVITE_VERIFY" -> {
                            val action =
                                VerifyEmailFragmentDirections
                                    .actionVerifyEmailFragmentToSetPasswordFragment(userId)
                            findNavController().navigate(action)
                        }

                        "RESET_PASSWORD" -> {
                            val action =
                                VerifyEmailFragmentDirections
                                    .actionVerifyEmailFragmentToChangePasswordFragment(userId)
                            findNavController().navigate(action)
                        }

                    }

                } catch (e: ApiException) {

                    if (e.code == 400) {
                        // Expected validation failure (wrong / expired OTP)
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.invalid_expired_code_message),
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.buttonVerify.isEnabled = true
                    } else {
                        // Real server issue
                        Toast.makeText(
                            requireContext(),
                            R.string.server_issue_message,
                            Toast.LENGTH_LONG
                        ).show()
                        binding.buttonVerify.isEnabled = true
                    }

                } catch (e: Exception) {
                    // Network / unexpected crash
                    Log.e("VerifyEmailFragment", e.message.toString())
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.message_unexpected_error),
                        Toast.LENGTH_LONG
                    ).show()
                    binding.buttonVerify.isEnabled = true
                }
            }
        }

        binding.authToolbar.setNavigationOnClickListener {
            showExitWarning()
        }

        // System back (same behavior)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitWarning()
                }
            }
        )

        binding.textBackToLogin.setOnClickListener {
            showLoginBackWarning()
        }

        binding.textResend.setOnClickListener {

            binding.textResend.isEnabled = false
            binding.textResend.alpha = 0.5f
            lifecycleScope.launch {
                try {
                    if (!isAdded) return@launch

                    val resp = userViewModel.resendEmailVerification(
                        userId, email, userName, purpose
                    )
                    emailOtpExpiresAt = resp.emailOtpExpiresAtMillis
                    startOtpFlow(emailOtpExpiresAt)

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.message_verification_code_resent),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: ApiException) {
                    if (!isAdded) return@launch

                    binding.textResend.isEnabled = true
                    binding.textResend.alpha = 1.0f

                    // Cooldown from server (HTTP 429)
                    if (e.code == 429) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_resend_wait),
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.textResend.isEnabled = true
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.message_unable_to_send_code),
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.textResend.isEnabled = true
                    }
                }
            }
        }
        val now = System.currentTimeMillis()

        if (emailOtpExpiresAt == 0L || emailOtpExpiresAt <= now) {
            resendOtpOnLoad()
        } else {
            startOtpFlow(emailOtpExpiresAt)
        }
    }
    private fun resendOtpOnLoad() {
        binding.textResend.isEnabled = false
        binding.textResend.alpha = 0.5f

        lifecycleScope.launch {
            try {
                val resp = userViewModel.resendEmailVerification(
                    userId, email, userName, purpose
                )

                emailOtpExpiresAt = resp.emailOtpExpiresAtMillis
                startOtpFlow(emailOtpExpiresAt)

                Toast.makeText(
                    requireContext(),
                    getString(R.string.message_verification_code_resent),
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.message_unable_to_send_code),
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("VerifyEmailFragment: Resend", e.message.toString() )
                // Allow manual retry
                binding.textResend.isEnabled = true
                binding.textResend.alpha = 1.0f
            }
        }
    }

    private fun onOtpExpired() {
        otpTimer?.cancel()
        otpTimer = null

        binding.textTimer.text = getString(R.string.code_expired_message)

        binding.buttonVerify.isEnabled = false
        binding.buttonVerify.alpha = 0.5f
    }

    private fun onOtpActive() {
        binding.buttonVerify.isEnabled = true
        binding.buttonVerify.alpha = 1.0f

        binding.textResend.isEnabled = false
        binding.textResend.isClickable = false
        binding.textResend.alpha = 0.5f
        binding.textTimer.visibility = View.VISIBLE
        binding.textResendTimer.visibility = View.VISIBLE
    }

    private fun startOtpFlow(otpExpiresAtMillis: Long) {
        startOtpValidityTimer(otpExpiresAtMillis)
        startResendCooldownTimer()
    }

    private fun startOtpValidityTimer(expiresAtMillis: Long) {
        val remaining = expiresAtMillis - System.currentTimeMillis()

        if (remaining <= 0) {
            onOtpExpired()
            return
        }

        otpTimer?.cancel()

        onOtpActive()

        otpTimer = object : CountDownTimer(remaining, 1000) {

            override fun onTick(ms: Long) {
                val min = (ms / 1000) / 60
                val sec = (ms / 1000) % 60
                binding.textTimer.text =
                    getString(R.string.code_expiry_timer).format(min, sec)
            }

            override fun onFinish() {
                onOtpExpired()
            }
        }.start()
    }

    private fun startResendCooldownTimer() {
        resendTimer?.cancel()

        onResendCooldownActive()

        resendTimer = object : CountDownTimer(ApnaBankConstants.RESEND_COOLDOWN_MS, 1000) {

            override fun onTick(ms: Long) {
                if (!isAdded) return
                val min = (ms / 1000) / 60
                val sec = (ms / 1000) % 60
                binding.textResendTimer.text =
                    getString(R.string.resend_available_timer).format(min, sec)
            }

            override fun onFinish() {
                onResendCooldownFinished()
            }
        }.start()
    }

    private fun onResendCooldownActive() {
        binding.textResend.isEnabled = false
        binding.textResend.isClickable = false
        binding.textResend.alpha = 0.5f
        binding.textResendTimer.visibility = View.VISIBLE
    }

    private fun onResendCooldownFinished() {
        resendTimer?.cancel()
        resendTimer = null

        binding.textResendTimer.visibility = View.GONE
        binding.textResend.isEnabled = true
        binding.textResend.isClickable = true
        binding.textResend.alpha = 1.0f
    }

    private fun showExitWarning() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.cancel_email_verify_message))
            .setMessage(
                getString(R.string.message_cancel_email_verify)
            )
            .setPositiveButton(R.string.text_leave) { _, _ ->
                findNavController().navigate(
                    VerifyEmailFragmentDirections.actionVerifyEmailFragmentToLoginFragment()
                )
            }
            .setNegativeButton(R.string.text_stay, null)
            .show()
    }

    private fun showLoginBackWarning() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.cancel_email_verify_message))
            .setMessage(
                getString(R.string.message_cancel_email_verify_login)
            )
            .setPositiveButton(R.string.text_leave) { _, _ ->

                findNavController().navigate(
                    VerifyEmailFragmentDirections.actionVerifyEmailFragmentToLoginFragment()
                )
            }
            .setNegativeButton(R.string.text_stay, null)
            .show()
    }

    override fun onDestroyView() {
        otpTimer?.cancel()
        otpTimer = null
        resendTimer?.cancel()
        resendTimer = null
        super.onDestroyView()
    }
}
