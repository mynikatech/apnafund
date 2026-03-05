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
import com.mynikatech.apnafund.databinding.FragmentChangePasswordBinding
import com.mynikatech.apnafund.net.ApiException
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.assessPasswordStrength
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val args: ChangePasswordFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
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
        binding.authToolbar.title = getString(R.string.text_change_password)


        binding.buttonChangePassword.setOnClickListener {
            val newPassword = binding.editTextNewPassword.text.toString()
            val confirmPassword = binding.editTextConfirmPassword.text.toString()

            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {

                if (newPassword.length < 8) {
                    Toast.makeText(
                        requireContext(),
                        "Password must be at least 8 characters",
                        Toast.LENGTH_SHORT
                    ).show()
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
                            ChangePasswordFragmentDirections
                                .actionChangePasswordFragmentToLoginFragment()
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
                                e.message ?: "Password update failed"
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            e.message ?: "Password update failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun showExitWarning() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel password reset?")
            .setMessage(
                "If you leave this screen, you will be redirected to the Login Screen. " +
                        "You’ll need to reverify your email again to continue."
            )
            .setPositiveButton("Leave") { _, _ ->
                findNavController().navigate(
                    ChangePasswordFragmentDirections.actionChangePasswordFragmentToLoginFragment()
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
