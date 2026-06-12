package com.mynikatech.apnafund.ui.auth

import android.os.Bundle
import android.util.Log
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
import com.mynikatech.apnafund.databinding.FragmentForgotPasswordPhoneBinding
import com.mynikatech.apnafund.net.ApiException
import com.mynikatech.apnafund.net.dto.Channel
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch

class ForgotPasswordPhoneFragment : Fragment() {

    private lateinit var binding: FragmentForgotPasswordPhoneBinding

    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            FragmentForgotPasswordPhoneBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSendResetCode.setOnClickListener {
            sendResetOtp()
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.authToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun sendResetOtp() {

        val phone =
            binding.editTextPhone.text.toString().trim()

        if (phone.length != 10) {

            showToast(
                getString(R.string.error_enter_valid_phone)
            )

            return
        }

        lifecycleScope.launch {

            binding.buttonSendResetCode.isEnabled = false

            try {

                val loginResponse =
                    userViewModel.getUserByPhone(phone)

                if (loginResponse?.user == null) {

                    showToast(
                        getString(
                            R.string.error_no_user_found_phone
                        )
                    )

                    binding.buttonSendResetCode.isEnabled = true
                    return@launch
                }
                val userDto =
                    loginResponse.user
                if (userDto == null) {
                    showToast(getString(R.string.error_user_not_found))
                    binding.buttonSendResetCode.isEnabled = true
                    return@launch
                }

                val user =
                    userDto.toEntity()

                val response = try {
                    userViewModel.sendOtp(
                        userId = user.userId,
                        phone = phone,
                        purpose = ApnaBankConstants.RESET_PASSWORD
                    )
                } catch (e: ApiException) {
                    Log.e("ForgotPasswordFragment", e.message.toString())
                    showToast(getString(R.string.error_server))
                    binding.buttonSendResetCode.isEnabled = true
                    return@launch
                }

                findNavController().navigate(
                    ForgotPasswordPhoneFragmentDirections
                        .actionForgotPasswordPhoneFragmentToVerifyOtpFragment(
                            userId = user.userId,
                            phoneNumber = phone,
                            otpExpiresAtMillis =
                                response.otpExpiresAtMillis,
                            purpose =
                                ApnaBankConstants.RESET_PASSWORD,
                            channel = Channel.WHATSAPP.name,
                            userName = user.fullName
                        )
                )

            } catch (e: ApiException) {

                showToast(
                    e.message
                        ?: getString(R.string.error_server)
                )

            } finally {

                binding.buttonSendResetCode.isEnabled = true
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}