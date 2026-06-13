package com.mynikatech.apnafund.ui.auth

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.databinding.FragmentVerifyOtpBinding
import com.mynikatech.apnafund.net.ApiException
import com.mynikatech.apnafund.net.dto.Channel
import com.mynikatech.apnafund.net.dto.OtpExpiryResponse
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import androidx.activity.OnBackPressedCallback

class VerifyOtpFragment : Fragment(R.layout.fragment_verify_otp) {

    private lateinit var binding: FragmentVerifyOtpBinding

    private val userViewModel: UserViewModel by viewModels()
    private val args: VerifyOtpFragmentArgs by navArgs()

    private var otpTimer: CountDownTimer? = null
    private var resendTimer: CountDownTimer? = null

    private var userId = 0
    private  var phoneNumber: String? = null
    private  var email: String? = null
    private lateinit var purpose: String
    private lateinit var channel: String
    private var shouldSetPin: Boolean = false
    private var userName: String? = null
    private var otpExpiresAtMillis = 0L
    private var isOtpPasteInProgress = false

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentVerifyOtpBinding.bind(view)

        userId = args.userId
        phoneNumber = args.phoneNumber
        email = args.email
        purpose = args.purpose
        otpExpiresAtMillis = args.otpExpiresAtMillis
        channel = args.channel
        userName = args.userName
        shouldSetPin = args.shouldSetPin
        binding.textDestination.text =
            when (channel) {
                Channel.EMAIL.name -> email.orEmpty()
                Channel.WHATSAPP.name -> phoneNumber.orEmpty()
                else -> ""
            }

        setupOtpInputs()
        setupClickListeners()

        val now = System.currentTimeMillis()

        if (otpExpiresAtMillis == 0L || otpExpiresAtMillis <= now) {
            resendOtpOnLoad()
        } else {
            startOtpFlow(otpExpiresAtMillis)
        }

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {

                    override fun handleOnBackPressed() {
                        showExitWarning()
                    }
                }
            )

    }

    private fun getOtp(): String {
        return buildString {
            append(binding.editOtp1.text?.toString().orEmpty())
            append(binding.editOtp2.text?.toString().orEmpty())
            append(binding.editOtp3.text?.toString().orEmpty())
            append(binding.editOtp4.text?.toString().orEmpty())
            append(binding.editOtp5.text?.toString().orEmpty())
            append(binding.editOtp6.text?.toString().orEmpty())
        }
    }

    private fun isOtpValid(): Boolean {
        return getOtp().length == 6
    }

    private fun verifyOtp(otp: String) {

        binding.buttonVerify.isEnabled = false

        lifecycleScope.launch {

            try {

                userViewModel.verifyOtp(
                    otp = otp,
                    userId = userId,
                    purpose = purpose,
                    channel = channel
                )

                showToast(
                    getString(R.string.successful_verification_message)
                )

                handleVerificationSuccess()

            } catch (e: ApiException) {

                binding.buttonVerify.isEnabled = true

                if (e.code == 400) {

                    showToast(
                        getString(
                            R.string.invalid_expired_code_message
                        )
                    )

                } else {

                    showToast(
                        getString(R.string.server_issue_message)
                    )
                }
            }
        }
    }

    private fun handleVerificationSuccess() {

        when (purpose) {

            ApnaBankConstants.RESET_PASSWORD -> {

                findNavController().navigate(
                    VerifyOtpFragmentDirections
                        .actionVerifyOtpFragmentToChangePasswordFragment(
                            userId
                        )
                )
            }

            ApnaBankConstants.LOGIN -> {

                findNavController().navigate(
                    VerifyOtpFragmentDirections
                        .actionVerifyOtpFragmentToLoginFragment(

                        )
                )
            }

            ApnaBankConstants.INVITE_VERIFY -> {

                findNavController().navigate(
                    VerifyOtpFragmentDirections
                        .actionVerifyOtpFragmentToSetPasswordFragment(
                            userId
                        )
                )
            }

            ApnaBankConstants.MODERATOR_REGISTER_VERIFY -> {

                if (shouldSetPin) {

                    findNavController().navigate(
                        VerifyOtpFragmentDirections
                            .actionVerifyOtpFragmentToSetPinFragment(
                                userId = userId,
                                isRegistrationFlow = true
                            )
                    )

                } else {

                    findNavController().navigate(
                        VerifyOtpFragmentDirections
                            .actionVerifyOtpFragmentToLoginFragment()
                    )
                }
            }
        }
    }

    private fun resendOtpOnLoad() {

        lifecycleScope.launch {

            try {

                val resp =
                    when (channel) {

                        Channel.WHATSAPP.name,
                        Channel.SMS.name -> {

                            userViewModel.sendOtp(
                                userId = userId,
                                phone = requireNotNull(phoneNumber),
                                purpose = purpose
                            )
                        }

                        Channel.EMAIL.name -> {

                            userViewModel.resendEmailVerification(
                                userId = userId,
                                email = requireNotNull(email),
                                userName = requireNotNull(userName),
                                purpose = purpose
                            )
                        }

                        else -> {
                            throw IllegalArgumentException(
                                "Unsupported channel: $channel"
                            )
                        }
                    }

                otpExpiresAtMillis =
                    resp.otpExpiresAtMillis

                startOtpFlow(otpExpiresAtMillis)

            } catch (e: Exception) {

                showToast(
                    getString(
                        R.string.message_unable_to_send_code
                    )
                )
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private fun setupClickListeners() {

        binding.buttonVerify.setOnClickListener {

            val otp = getOtp()

            if (otp.length != 6) {
                showToast(
                    getString(R.string.error_enter_valid_6_digit_code)
                )
                return@setOnClickListener
            }

            verifyOtp(otp)
        }

        binding.textResend.setOnClickListener {
            resendOtp()
        }

        binding.authToolbar.setNavigationOnClickListener {
            showExitWarning()
        }

        binding.textBackToLogin.setOnClickListener {
            showLoginBackWarning()
        }
    }

    private fun setupOtpInputs() {

        updateVerifyButtonState()

        val fields = listOf(
            binding.editOtp1,
            binding.editOtp2,
            binding.editOtp3,
            binding.editOtp4,
            binding.editOtp5,
            binding.editOtp6
        )

        fields.forEachIndexed { index, editText ->

            editText.addTextChangedListener(object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) = Unit

                override fun afterTextChanged(s: Editable?) {

                    if (isOtpPasteInProgress) return

                    val value = s?.toString().orEmpty()

                    if (value.length > 1) {

                        val digits = value.filter { it.isDigit() }

                        if (digits.length == 6) {

                            isOtpPasteInProgress = true

                            binding.editOtp1.setText(digits[0].toString())
                            binding.editOtp2.setText(digits[1].toString())
                            binding.editOtp3.setText(digits[2].toString())
                            binding.editOtp4.setText(digits[3].toString())
                            binding.editOtp5.setText(digits[4].toString())
                            binding.editOtp6.setText(digits[5].toString())

                            isOtpPasteInProgress = false

                            binding.editOtp6.requestFocus()

                            updateVerifyButtonState()

                            return
                        }
                    }

                    if (
                        value.length == 1 &&
                        index < fields.lastIndex
                    ) {
                        fields[index + 1].requestFocus()
                    }

                    updateVerifyButtonState()
                }
            })

            editText.setOnKeyListener { _, keyCode, event ->

                if (
                    keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    editText.text.isNullOrEmpty() &&
                    index > 0
                ) {
                    fields[index - 1].requestFocus()
                    fields[index - 1].setSelection(
                        fields[index - 1].text?.length ?: 0
                    )
                }

                false
            }
        }

        binding.editOtp1.requestFocus()
    }

    private fun updateVerifyButtonState() {

        val enabled = getOtp().length == 6

        binding.buttonVerify.isEnabled = enabled
        binding.buttonVerify.alpha =
            if (enabled) 1f else 0.5f
    }

    private fun startOtpFlow(
        otpExpiresAtMillis: Long
    ) {
        startOtpValidityTimer(
            otpExpiresAtMillis
        )

        startResendCooldownTimer()
    }

    private fun startOtpValidityTimer(
        expiresAtMillis: Long
    ) {

        val remaining =
            expiresAtMillis - System.currentTimeMillis()

        if (remaining <= 0) {
            onOtpExpired()
            return
        }

        otpTimer?.cancel()

        onOtpActive()

        otpTimer = object : CountDownTimer(
            remaining,
            1000
        ) {

            override fun onTick(ms: Long) {

                val min = (ms / 1000) / 60
                val sec = (ms / 1000) % 60

                binding.textTimer.text =
                    getString(
                        R.string.code_expiry_timer
                    ).format(min, sec)
            }

            override fun onFinish() {
                onOtpExpired()
            }

        }.start()
    }

    private fun startResendCooldownTimer() {

        resendTimer?.cancel()

        onResendCooldownActive()

        resendTimer = object : CountDownTimer(
            ApnaBankConstants.RESEND_COOLDOWN_MS,
            1000
        ) {

            override fun onTick(ms: Long) {

                if (!isAdded) return

                val min = (ms / 1000) / 60
                val sec = (ms / 1000) % 60

                binding.textResendTimer.text =
                    getString(
                        R.string.resend_available_timer
                    ).format(min, sec)
            }

            override fun onFinish() {
                onResendCooldownFinished()
            }

        }.start()
    }
    private fun onOtpActive() {

        binding.textTimer.visibility =
            View.VISIBLE

        binding.textResendTimer.visibility =
            View.VISIBLE

        binding.textResend.isEnabled = false
        binding.textResend.alpha = 0.5f

        binding.buttonVerify.isEnabled =
            getOtp().length == 6

        binding.buttonVerify.alpha =
            if (getOtp().length == 6) 1f else 0.5f
    }
    private fun onOtpExpired() {

        otpTimer?.cancel()
        otpTimer = null

        binding.textTimer.text =
            getString(
                R.string.code_expired_message
            )

        binding.buttonVerify.isEnabled = false
        binding.buttonVerify.alpha = 0.5f
    }
    private fun onResendCooldownActive() {

        binding.textResend.isEnabled = false
        binding.textResend.isClickable = false
        binding.textResend.alpha = 0.5f

        binding.textResendTimer.visibility =
            View.VISIBLE
    }
    private fun onResendCooldownFinished() {

        resendTimer?.cancel()
        resendTimer = null

        binding.textResendTimer.visibility =
            View.GONE

        binding.textResend.isEnabled = true
        binding.textResend.isClickable = true
        binding.textResend.alpha = 1f
    }

    override fun onDestroyView() {

        otpTimer?.cancel()
        otpTimer = null

        resendTimer?.cancel()
        resendTimer = null

        super.onDestroyView()
    }

    private fun resendOtp() {

        binding.textResend.isEnabled = false

        lifecycleScope.launch {

            try {

                val resp: OtpExpiryResponse =
                    when (channel) {

                        Channel.EMAIL.name -> {
                            userViewModel.resendEmailVerification(
                                userId = userId,
                                email = requireNotNull(email),
                                userName = requireNotNull(userName),
                                purpose = purpose
                            )
                        }

                        Channel.WHATSAPP.name,
                        Channel.SMS.name -> {
                            userViewModel.sendOtp(
                                userId = userId,
                                phone = requireNotNull(phoneNumber),
                                purpose = purpose
                            )
                        }

                        else -> throw IllegalArgumentException(
                            "Unsupported channel: $channel"
                        )
                    }

                otpExpiresAtMillis =
                    resp.otpExpiresAtMillis

                clearOtpFields()

                startOtpFlow(
                    otpExpiresAtMillis
                )

                showToast(
                    getString(
                        R.string.message_verification_code_resent
                    )
                )

            } catch (e: Exception) {

                binding.textResend.isEnabled = true

                showToast(
                    getString(
                        R.string.message_unable_to_send_code
                    )
                )
            }
        }
    }

    private fun showExitWarning() {

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(
                getString(
                    R.string.cancel_verification_message
                )
            )
            .setMessage(
                getString(
                    R.string.message_cancel_verification
                )
            )
            .setPositiveButton(
                android.R.string.yes
            ) { _, _ ->

                findNavController().popBackStack()
            }
            .setNegativeButton(
                android.R.string.no,
                null
            )
            .show()
    }

    private fun showLoginBackWarning() {

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(
                getString(
                    R.string.cancel_verification_message
                )
            )
            .setMessage(
                getString(
                    R.string.message_cancel_verification_login
                )
            )
            .setPositiveButton(
                android.R.string.yes
            ) { _, _ ->

                findNavController().navigate(
                    R.id.loginFragment
                )
            }
            .setNegativeButton(
                android.R.string.no,
                null
            )
            .show()
    }

    private fun clearOtpFields() {

        binding.editOtp1.text?.clear()
        binding.editOtp2.text?.clear()
        binding.editOtp3.text?.clear()
        binding.editOtp4.text?.clear()
        binding.editOtp5.text?.clear()
        binding.editOtp6.text?.clear()

        binding.editOtp1.requestFocus()

        binding.buttonVerify.isEnabled = false
        binding.buttonVerify.alpha = 0.5f
    }
}