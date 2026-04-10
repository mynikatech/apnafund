package com.mynikatech.apnafund.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.FragmentResetPasswordBinding
import com.mynikatech.apnafund.net.ApiException
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.assessPasswordStrength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResetPasswordFragment : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!

    private val args: ResetPasswordFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        binding.authToolbar.title = getString(R.string.text_reset_password)


        binding.buttonChangePassword.setOnClickListener {
            binding.buttonChangePassword.isEnabled = false
            val userId = args.userId

            val currentPassword = binding.editTextCurrentPassword.text.toString()
            val newPassword = binding.editTextNewPassword.text.toString()
            val confirmPassword = binding.editTextConfirmPassword.text.toString()

            if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // validate current password

            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT)
                    .show()
                binding.buttonChangePassword.isEnabled = true
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val localUser = userViewModel.fetchUser(userId)
                if (localUser == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "User Not found", Toast.LENGTH_SHORT)
                            .show()
                    }
                    binding.buttonChangePassword.isEnabled = true
                    return@launch
                }
                val isPasswordMatch =
                    Converters.verifyPassword(currentPassword, localUser.passwordHash)

                if (!isPasswordMatch) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Incorrect Current Password",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    binding.buttonChangePassword.isEnabled = true
                    return@launch
                }
                if (newPassword.length < 8) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Password must be at least 8 characters",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    binding.buttonChangePassword.isEnabled = true
                    return@launch
                }
                val strength = assessPasswordStrength(newPassword)
                if (strength.name == "WEAK") {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Password is too weak. Please add special symbols @,*,\$ etc and numbers",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    binding.buttonChangePassword.isEnabled = true
                    return@launch
                }

                lifecycleScope.launch {
                    try {
                        userViewModel.changeUserPassword(args.userId, newPassword)

                        Toast.makeText(
                            requireContext(),
                            "Password changed successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        findNavController().navigate(
                            ResetPasswordFragmentDirections
                                .actionResetPasswordFragmentToLoginFragment()
                        )
                    } catch (e: ApiException) {
                        Log.e("Apnafund", "Exception, ${e.type}")
                        // 🎯 Handle known server-side validation errors
                        val message = when (e.type) {
                            "PASSWORD_REUSED" ->
                                "You cannot reuse your last 3 passwords"

                            "NETWORK_ERROR" ->
                                "Please check your internet connection"

                            "SERVER_ERROR" ->
                                "Server error. Please try again later"

                            else ->
                                e.message
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                        binding.buttonChangePassword.isEnabled = true
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            e.message ?: "Password update failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.buttonChangePassword.isEnabled = true
                    }
                }
            }
        }
    }

    private fun showExitWarning() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel password reset?")
            .setMessage(
                "If you reset your password again later."
            )
            .setPositiveButton("Leave") { _, _ ->
                findNavController().navigate(
                    ResetPasswordFragmentDirections.actionResetPasswordFragmentToSettingsFragment()
                )
            }
            .setNegativeButton("Stay", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
