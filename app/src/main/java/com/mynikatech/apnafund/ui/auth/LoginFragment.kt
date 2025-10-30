// LoginFragment.kt
package com.mynikatech.apnafund.ui.auth

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.FragmentLoginBinding
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.Converters
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private var storedVerificationId: String? = null
    private var isPhoneLogin = false

    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FirebaseApp.initializeApp(requireContext())
        auth = FirebaseAuth.getInstance()
        binding.phoneEditText.filters = arrayOf(
            InputFilter.LengthFilter(10),
            InputFilter { source, start, end, dest, dstart, dend ->
                val result = dest.toString().substring(0, dstart) +
                        source.subSequence(start, end) +
                        dest.toString().substring(dend)
                if (!result.matches(Regex("^\\d{0,10}$"))) return@InputFilter ""
                if (result.startsWith("0")) return@InputFilter ""
                null
            }
        )
        binding.toggleLoginMode.setOnClickListener {
            isPhoneLogin = !isPhoneLogin
            updateLoginMode()
        }

        binding.loginButton.setOnClickListener {
            if (isPhoneLogin) {
                val phone = binding.phoneEditText.text.toString()
                if (phone.isEmpty()) {
                    Toast.makeText(requireContext(), "Phone number required", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }
                lifecycleScope.launch {
                    val localUser = userViewModel.getUserByPhone(phone)
                    if (localUser == null) {
                        Toast.makeText(
                            requireContext(),
                            "User not found. Please register first",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                }
                sendOtp(phone)
            } else {
                val email = binding.emailEditText.text.toString()
                val password = binding.passwordEditText.text.toString()
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Email and Password required",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    loginWithEmail(email, password)
                }
            }

        }

        binding.verifyOtpButton.setOnClickListener {
            val otp = binding.otpEditText.text.toString()
            val phone = binding.phoneEditText.text.toString()
            if (otp.isNotEmpty() && storedVerificationId != null) {
                val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, otp)
                signInWithPhoneAuthCredential(credential, phone)
            }
        }

        binding.registerRedirectText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        binding.textForgotPassword.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please enter your email to reset password",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Option 1: Firebase password reset
            /*FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    /*Toast.makeText(
                        requireContext(),
                        "Password reset email sent. Check your inbox.",
                        Toast.LENGTH_SHORT
                    ).show()*/
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Failed to send reset email: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }*/
            // doing both at this point of time
            val action = LoginFragmentDirections
                .actionLoginFragmentToForgotPasswordFragment(email)
            findNavController().navigate(action)
        }
    }

    private fun updateLoginMode() {
        binding.emailLayout.visibility = if (isPhoneLogin) View.GONE else View.VISIBLE
        binding.passwordLayout.visibility = if (isPhoneLogin) View.GONE else View.VISIBLE
        binding.phoneLayout.visibility = if (isPhoneLogin) View.VISIBLE else View.GONE
        binding.otpLayout.visibility = if (isPhoneLogin) View.VISIBLE else View.GONE
        binding.verifyOtpButton.visibility = if (isPhoneLogin) View.VISIBLE else View.GONE
        binding.toggleLoginMode.text = if (isPhoneLogin) "Use Email/Password" else "Use Phone OTP"
    }

    private fun loginWithEmail(email: String, password: String) {
        lifecycleScope.launch {
            val localUser = userViewModel.getUserByEmail(email)
            if (localUser == null) {
                Toast.makeText(
                    requireContext(),
                    "User not found. Please register first",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            val userPassword = localUser.passwordHash
            if (userPassword.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Password is not set. Please set password.",
                    Toast.LENGTH_SHORT
                ).show()
                val action = LoginFragmentDirections
                    .actionLoginFragmentToSetPasswordFragment(localUser.userId)
                findNavController().navigate(action)
                return@launch
            }
            val isPasswordMatch = Converters.verifyPassword(password, localUser.passwordHash)

            if (!isPasswordMatch) {
                Toast.makeText(requireContext(), "Incorrect Password", Toast.LENGTH_SHORT).show()
                return@launch
            }
            /*if (localUser.firebaseUserId.isNullOrBlank()) {
                // Firebase UID is not yet set — allow user to setup password
                val action = LoginFragmentDirections
                    .actionLoginFragmentToSetPasswordFragment(localUser.userId, email)
                findNavController().navigate(action)
                return@launch
            }*/
            // Proceed with Firebase sign-in only if password is not empty
            lifecycleScope.launch {
                val isDue = userViewModel.isPasswordRotationDue(localUser.userId)
                if (isDue) {
                    val action = LoginFragmentDirections
                        .actionLoginFragmentToChangePasswordFragment(localUser.userId)
                    findNavController().navigate(action)
                } else {
                    val action = LoginFragmentDirections
                        .actionLoginFragmentToUserSummaryFragment(localUser.userId)
                    findNavController().navigate(action)
                }
            }
            /*
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
               if (task.isSuccessful) {
                    lifecycleScope.launch {
                        val isDue = userViewModel.isPasswordRotationDue(localUser.userId)
                        if (isDue) {
                            val action = LoginFragmentDirections
                                .actionLoginFragmentToChangePasswordFragment(localUser.userId)
                            findNavController().navigate(action)
                        } else {
                            val action = LoginFragmentDirections
                                .actionLoginFragmentToUserSummaryFragment(localUser.userId)
                            findNavController().navigate(action)
                        }
                    }
                } else {
                    val exceptionMessage = task.exception?.message ?: ""

                    if (exceptionMessage.contains("user-not-found", ignoreCase = true) ||
                        exceptionMessage.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
                        exceptionMessage.contains("There is no user record", ignoreCase = true) ||
                        exceptionMessage.contains("password is invalid", ignoreCase = true)
                    ) {
                        // Firebase account not created yet for existing app user
                        Toast.makeText(
                            requireContext(),
                            "No Firebase password set. Please set your password.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Redirect to SetPasswordFragment (or ChangePasswordFragment) to set it
                        val action = LoginFragmentDirections
                            .actionLoginFragmentToChangePasswordFragment(localUser.userId)
                        findNavController().navigate(action)
                    } else if (exceptionMessage.contains(
                            "auth credential is incorrect, malformed or has expired",
                            ignoreCase = true
                        )
                    ) {
                        Toast.makeText(
                            requireContext(),
                            "Incorrect Password.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Login failed: $exceptionMessage",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }*/
        }
    }

    private fun sendOtp(phone: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phone")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential, phone)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(
                        requireContext(),
                        "Verification failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    storedVerificationId = verificationId
                    Toast.makeText(requireContext(), "OTP sent", Toast.LENGTH_SHORT).show()
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, phone: String) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    lifecycleScope.launch {
                        val localUser = userViewModel.getUserByPhone(phone)
                        if (localUser == null) {
                            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT)
                                .show()
                            return@launch
                        }
                        val isDue = userViewModel.isPasswordRotationDue(localUser.userId)

                        if (isDue) {
                            // Navigate to ChangePasswordFragment, passing userId
                            val action = LoginFragmentDirections
                                .actionLoginFragmentToChangePasswordFragment(localUser.userId)
                            findNavController().navigate(action)
                        } else {
                            // Go to user summary screen with userId
                            val action = LoginFragmentDirections
                                .actionLoginFragmentToUserSummaryFragment(localUser.userId)
                            findNavController().navigate(action)
                        }
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
