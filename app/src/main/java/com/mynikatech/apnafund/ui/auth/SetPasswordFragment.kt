package com.mynikatech.apnafund.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.mynikatech.apnafund.databinding.FragmentSetPasswordBinding
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.assessPasswordStrength
import kotlinx.coroutines.launch

class SetPasswordFragment : Fragment() {

    private lateinit var binding: FragmentSetPasswordBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var userEmail: String
    private var userId: Int = -1
    private val userViewModel: UserViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetPasswordBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        arguments?.let {
            userEmail = it.getString("emailId") ?: ""
            userId = it.getInt("userId")
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textViewEmail.text = userEmail

        binding.buttonSubmit.setOnClickListener {
            val password = binding.editTextPassword.text.toString()
            val confirm = binding.editTextConfirmPassword.text.toString()

            if (password.length < 8) {
                Toast.makeText(
                    requireContext(),
                    "Password must be at least 8 characters",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val strength = assessPasswordStrength(password)
            if (strength.name == "WEAK") {
                Toast.makeText(
                    requireContext(),
                    "Password is too weak. Please add special symbols @,*,$ etc and numbers",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                // Update the existing user with Firebase UID
                val user = userViewModel.fetchUser(userId)
                if (user != null) {
                    userViewModel.changeUserPassword(userId, password)
                    Toast.makeText(
                        requireContext(),
                        "Password set successfully. Relogin",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(
                        SetPasswordFragmentDirections.actionSetPasswordFragmentToLoginFragment()
                    )
                } else {
                    // some exception todo
                }
            }
            /*
            auth.createUserWithEmailAndPassword(userEmail, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUid = auth.currentUser?.uid
                        if (firebaseUid != null) {
                            lifecycleScope.launch {
                                // Update the existing user with Firebase UID
                                val user = userViewModel.fetchUser(userId)
                                if (user != null) {
                                    val updatedUser = user.copy(firebaseUserId = firebaseUid)
                                    userViewModel.saveOrUpdateUser(updatedUser)
                                }
                            }
                        }

                        Toast.makeText(
                            requireContext(),
                            "Password set successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        findNavController().navigate(
                            SetPasswordFragmentDirections.actionSetPasswordFragmentToLoginFragment()
                        )
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Error: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } */
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().navigate(
                SetPasswordFragmentDirections.actionSetPasswordFragmentToLoginFragment()
            )
        }
    }
}
