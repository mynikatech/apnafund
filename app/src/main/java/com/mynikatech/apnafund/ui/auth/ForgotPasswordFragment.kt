package com.mynikatech.apnafund.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mynikatech.apnafund.databinding.FragmentForgotPasswordBinding
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.assessPasswordStrength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordFragment : Fragment() {

    private lateinit var binding: FragmentForgotPasswordBinding
    private val userViewModel: UserViewModel by viewModels()
    private lateinit var userEmail: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        arguments?.let {
            userEmail = it.getString("userEmail") ?: ""
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.editTextEmail.setText(userEmail)

        binding.editTextNewPassword.addTextChangedListener { validatePasswordInputs() }
        binding.editTextConfirmPassword.addTextChangedListener { validatePasswordInputs() }

        binding.buttonResetPassword.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val newPassword = binding.editTextNewPassword.text.toString()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter email Id", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Enter valid email address", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val user = userViewModel.getUserByEmail(email)
                if (user == null) {
                    Toast.makeText(
                        requireContext(),
                        "No user found with this email",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                if (!validatePasswordInputs()) return@launch
                val isReused = userViewModel.isPasswordReused(user.userId, newPassword)
                if (isReused) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Cannot reuse last 3 passwords",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }
                userViewModel.changeUserPassword(user.userId, newPassword)
                Toast.makeText(
                    requireContext(),
                    "Password updated. Please login again.",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().navigateUp()
            }
        }
        binding.buttonCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun validatePasswordInputs(): Boolean {
        val password = binding.editTextNewPassword.text.toString()
        val confirmPassword = binding.editTextConfirmPassword.text.toString()
        if (password.isEmpty()) {
            binding.editTextNewPassword.error = null
        }
        if (confirmPassword.isEmpty()) {
            binding.editTextConfirmPassword.error = null
        }
        // Basic length check
        if (password.isNotEmpty() && password.length < 8) {
            binding.editTextNewPassword.error = "Password must be at least 8 characters"
            return false
        }
        // Password strength
        val strength = assessPasswordStrength(password)
        if (password.isNotEmpty() && strength.name == "WEAK") {
            binding.editTextNewPassword.error =
                "Password is too weak. Use letters, numbers, and special characters"
            return false
        }
        // Confirm password match
        if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
            binding.editTextConfirmPassword.error = "Passwords do not match"
            return false
        }
        // Clear errors if valid
        binding.editTextNewPassword.error = null
        binding.editTextConfirmPassword.error = null
        return true
    }
}
