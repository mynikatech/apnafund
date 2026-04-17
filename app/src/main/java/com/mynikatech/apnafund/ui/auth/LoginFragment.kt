// LoginFragment.kt
package com.mynikatech.apnafund.ui.auth

import android.os.Bundle
import android.text.InputFilter
import android.util.Log
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
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.mappers.toEntity
import com.mynikatech.apnafund.databinding.FragmentLoginBinding
import com.mynikatech.apnafund.net.ApiException
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
            binding.loginButton.isEnabled = false
            if (isPhoneLogin) {
                val phone = binding.phoneEditText.text.toString()
                if (phone.isEmpty()) {
                    Toast.makeText(requireContext(),
                        getString(R.string.error_phone_number_required), Toast.LENGTH_SHORT)
                        .show()
                    binding.loginButton.isEnabled = true
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    val loginResponse = try {
                        userViewModel.getUserByPhone(phone)
                    } catch (e: ApiException) {
                        when (e.code) {
                            404 -> showToast(getString(R.string.error_user_not_found_register_first))
                            400 -> showToast(e.message)
                            else -> showToast(getString(R.string.error_server))
                        }
                        binding.loginButton.isEnabled = true
                        return@launch
                    }
                    if (null == loginResponse) {
                        binding.loginButton.isEnabled = true
                        return@launch
                    }

                    val localUser = loginResponse.user.toEntity()

                    if (localUser == null) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_user_not_found_register_first),
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.otpLayout.visibility = View.GONE
                        binding.verifyOtpButton.visibility = View.GONE
                        binding.loginButton.isEnabled = true
                        return@launch
                    }
                    binding.otpLayout.visibility = View.VISIBLE
                    binding.verifyOtpButton.visibility = View.VISIBLE
                    binding.cancelOtpButton.visibility = View.VISIBLE
                    sendOtp(phone)
                }

            } else {
                val email = binding.emailEditText.text.toString()
                val password = binding.passwordEditText.text.toString()
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_email_and_pwd_required),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.loginButton.isEnabled = true
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
                binding.otpLayout.visibility = View.GONE
                binding.verifyOtpButton.visibility = View.GONE
                signInWithPhoneAuthCredential(credential, phone)
            }
        }

        binding.cancelOtpButton.setOnClickListener {

            // Reset phone-login state
            isPhoneLogin = false

            // Clear fields
            binding.phoneEditText.text?.clear()
            binding.otpEditText.text?.clear()

            //  Hide OTP UI
            binding.otpLayout.visibility = View.GONE
            binding.verifyOtpButton.visibility = View.GONE
            binding.cancelOtpButton.visibility = View.GONE

            // Re-enable login button (in case it was disabled)
            binding.loginButton.isEnabled = true

            // Switch back to email login UI
            updateLoginMode()
        }

        binding.registerRedirectText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        binding.textForgotPassword.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_enter_email_reset_password),
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
        // OTP MUST ALWAYS START HIDDEN
        binding.otpLayout.visibility = View.GONE
        binding.verifyOtpButton.visibility = View.GONE
        binding.cancelOtpButton.visibility = if (isPhoneLogin) View.VISIBLE else View.GONE
        binding.toggleLoginMode.text = if (isPhoneLogin) {
            getString(R.string.text_use_email_password)
        } else {
            getString(R.string.text_use_phone_otp)
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        lifecycleScope.launch {
            val loginResponse = try {
                userViewModel.getUserByEmail(email)
            } catch (e: ApiException) {
                when (e.code) {
                    404 -> showToast(getString(R.string.error_user_not_found_register_first))
                    400 -> showToast(e.message)
                    else -> showToast(getString(R.string.error_server))
                }
                binding.loginButton.isEnabled = true
                return@launch
            }
            if (null == loginResponse) {
                binding.loginButton.isEnabled = true
                return@launch
            }


            val localUser = loginResponse.user.toEntity()
            val firebaseToken = loginResponse.firebaseToken



            if (localUser == null) {
                showToast(getString(R.string.user_not_found_register_norml_user))
                binding.loginButton.isEnabled = true
                return@launch
            }

            val userPassword = localUser.passwordHash
            if (userPassword.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_password_not_set),
                    Toast.LENGTH_SHORT
                ).show()
                val action = LoginFragmentDirections
                    .actionLoginFragmentToSetPasswordFragment(localUser.userId)
                findNavController().navigate(action)
                binding.loginButton.isEnabled = true
                return@launch
            }
            val isPasswordMatch = Converters.verifyPassword(password, localUser.passwordHash)

            if (!isPasswordMatch) {
                Toast.makeText(requireContext(),
                    getString(R.string.error_incorrect_password), Toast.LENGTH_SHORT).show()
                binding.loginButton.isEnabled = true
                return@launch
            }
            FirebaseAuthHelper.ensureFirebaseSignedIn(
                firebaseToken = firebaseToken,
                onSuccess = {
                    lifecycleScope.launch {
                        val isDue = userViewModel.isPasswordRotationDue(localUser.userId)
                        if (isDue) {
                            findNavController().navigate(
                                LoginFragmentDirections
                                    .actionLoginFragmentToChangePasswordFragment(localUser.userId)
                            )
                        } else {
                            val isVerified = userViewModel.isEmailVerified(localUser.userId)

                            if (!isVerified) {
                                val resp = try {
                                    userViewModel.resendEmailVerification(
                                        userId = localUser.userId,
                                        email = email,
                                        userName = "${localUser.firstName} ${localUser.lastName}",
                                        purpose = ApnaBankConstants.TEXT_EMAIL_VERIFY
                                    )
                                } catch (e: ApiException) {
                                    Log.e("LoginFragment", e.message.toString())
                                    showToast(getString(R.string.error_server))
                                    binding.loginButton.isEnabled = true
                                    return@launch
                                }
                                findNavController().navigate(
                                    LoginFragmentDirections
                                        .actionLoginFragmentToVerifyEmailFragment(
                                            userId = localUser.userId,
                                            email = email,
                                            userName = "${localUser.firstName} ${localUser.lastName}",
                                            emailOtpExpiresAtMillis = resp.emailOtpExpiresAtMillis,
                                            purpose = ApnaBankConstants.TEXT_EMAIL_VERIFY
                                        )
                                )
                            } else {
                                FirebaseAuth.getInstance().currentUser
                                    ?.getIdToken(true)
                                    ?.addOnSuccessListener {
                                        Log.d("FirebaseAuth", "Token claims = ${it.claims}")
                                    }
                                findNavController().navigate(
                                    LoginFragmentDirections
                                        .actionLoginFragmentToUserSummaryFragment(localUser.userId)
                                )
                            }

                        }
                    }
                },
                onFailure = {
                    binding.loginButton.isEnabled = true
                    showToast(getString(R.string.error_authentication_failed))
                }
            )
            /*if (localUser.firebaseUserId.isNullOrBlank()) {
                // Firebase UID is not yet set — allow user to setup password
                val action = LoginFragmentDirections
                    .actionLoginFragmentToSetPasswordFragment(localUser.userId, email)
                findNavController().navigate(action)
                return@launch
            }*/
            // Proceed with Firebase sign-in only if password is not empty

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
                    Log.d("OTP_DEBUG", "onVerificationCompleted (auto-verification)")
                    signInWithPhoneAuthCredential(credential, phone)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("OTP_DEBUG", "FAILED before verification (Firebase rejected app)", e)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_verification_failed, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.loginButton.isEnabled = true
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d("OTP_DEBUG", "OTP SENT, verificationId=$verificationId")
                    storedVerificationId = verificationId
                    binding.loginButton.isEnabled = false
                    binding.loginButton.postDelayed({
                        if (isAdded) { // fragment safety
                            binding.loginButton.isEnabled = true
                        }
                    }, 60_000)
                    Toast.makeText(requireContext(),
                        getString(R.string.message_otp_sent), Toast.LENGTH_SHORT).show()
                    Log.d("OTP_DEBUG", "currentUser=${FirebaseAuth.getInstance().currentUser}")
                }
            }).build()

        FirebaseAuth.getInstance().firebaseAuthSettings
            .forceRecaptchaFlowForTesting(true)

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, phone: String) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("AUTH", "Firebase OTP failed", task.exception)
                    showToast(getString(R.string.error_otp_verification_failed))
                    binding.loginButton.isEnabled = true
                    return@addOnCompleteListener
                }

                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser == null) {
                    showToast(getString(R.string.error_authentication_failed))
                    binding.loginButton.isEnabled = true
                    return@addOnCompleteListener
                }

                val isPhoneUser = firebaseUser.providerData
                    .any { it.providerId == PhoneAuthProvider.PROVIDER_ID }

                if (!isPhoneUser) {
                    Log.e("OTP_DEBUG", "User is not authenticated via PHONE")
                    FirebaseAuth.getInstance().signOut()
                    showToast(getString(R.string.error_phone_verification_failed))
                    binding.loginButton.isEnabled = true
                    return@addOnCompleteListener
                }

                val firebaseUid = firebaseUser.uid
                Log.d("OTP_DEBUG", "OTP VERIFIED, uid=$firebaseUid")

                lifecycleScope.launch {
                    val loginResponse = try {
                        userViewModel.getUserByPhone(phone)
                    } catch (e: ApiException) {
                        when (e.code) {
                            404 -> showToast(getString(R.string.error_user_not_found_register_first))
                            400 -> showToast(e.message)
                            else -> showToast(getString(R.string.error_server))
                        }
                        binding.loginButton.isEnabled = true
                        return@launch
                    }
                    if (null == loginResponse) {
                        binding.loginButton.isEnabled = true
                        return@launch
                    }

                    val localUser = loginResponse.user.toEntity()
                    if (localUser == null) {
                        Toast.makeText(requireContext(), getString(R.string.error_user_not_found), Toast.LENGTH_SHORT)
                            .show()
                        FirebaseAuth.getInstance().signOut()
                        binding.loginButton.isEnabled = true
                        return@launch
                    }

                    // MUST succeed or STOP
                    val updated = userViewModel.updateFirebaseUserIdSafely(
                        userId = localUser.userId,
                        firebaseUid = firebaseUid
                    )

                    if (!updated) {
                        showToast(getString(R.string.error_login_failed_contact_support))
                        FirebaseAuth.getInstance().signOut()
                        binding.loginButton.isEnabled = true
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

            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun ensureFirebaseAuth(onDone: () -> Unit) {
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            onDone()
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener {
                Log.d("FirebaseAuth", "Anonymous UID = ${it.user?.uid}")
                onDone()
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_authentication_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
