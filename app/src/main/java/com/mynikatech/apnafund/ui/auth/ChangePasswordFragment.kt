package com.mynikatech.apnafund.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mynikatech.apnafund.databinding.FragmentChangePasswordBinding
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

                val isReused = userViewModel.isPasswordReused(args.userId, newPassword)
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

                userViewModel.changeUserPassword(args.userId, newPassword)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Password changed successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(ChangePasswordFragmentDirections.actionChangePasswordFragmentToLoginFragment())
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
