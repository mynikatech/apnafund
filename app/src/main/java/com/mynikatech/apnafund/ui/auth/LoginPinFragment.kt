package com.mynikatech.apnafund.ui.auth

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mynikatech.apnafund.databinding.FragmentLoginPinBinding
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch

class LoginPinFragment : Fragment() {

    private lateinit var binding: FragmentLoginPinBinding
    private var userId: Int = -1
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginPinBinding.inflate(inflater, container, false)
        arguments?.let {
            userId = it.getInt("userId")
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAutoMove(binding.loginPinBoxes)
        lifecycleScope.launch {
            val user = userViewModel.fetchUser(userId)
            binding.textLoggedInUser.text = "${user?.firstName} ${user?.lastName}"
        }
        binding.buttonSubmit.setOnClickListener {
            binding.buttonSubmit.isEnabled = false
            val pin = getPinFromBoxes(binding.loginPinBoxes)
            if (pin.length != 4) {
                showToast("Please enter 4-digit PIN")
                binding.buttonSubmit.isEnabled = true
                return@setOnClickListener
            }
            // Save PIN securely
            lifecycleScope.launch {

                if (userViewModel.checkUserPIN(userId, pin)) {

                    if (userViewModel.isPINRotationDue(userId)) {
                        showToast("PIN has expired. Please reset")
                        // forward to change pin fragment/screen
                        val action = LoginPinFragmentDirections
                            .actionLoginPinFragmentToSetPinFragment(userId)
                        findNavController().navigate(action)
                    } else {
                        // 🔐 STEP 1: Get Firebase token from backend
                        val firebaseRespToken = try {
                            userViewModel.getFirebaseTokenForUser(userId)
                        } catch (e: Exception) {
                            showToast("Authentication failed. Please login again.")
                            findNavController().navigate(
                                LoginPinFragmentDirections.actionLoginPinFragmentToLoginFragment()
                            )
                            binding.buttonSubmit.isEnabled = true
                            return@launch
                        }

                        // STEP 2: Ensure Firebase is signed in
                        FirebaseAuthHelper.ensureFirebaseSignedIn(
                            firebaseToken = firebaseRespToken.firebaseToken,
                            onSuccess = {
                                SessionManager.isFirebaseSynced = true

                                // STEP 3: Navigate only AFTER Firebase is ready
                                findNavController().navigate(
                                    LoginPinFragmentDirections
                                        .actionLoginPinFragmentToUserSummaryFragment(userId)
                                )
                            },
                            onFailure = {
                                binding.buttonSubmit.isEnabled = true
                                showToast("Chat connection failed. Please retry.")
                            }
                        )
                    }
                } else {
                    showToast("Incorrect PIN entered")
                    binding.buttonSubmit.isEnabled = true
                    return@launch
                }
            }
        }
        binding.buttonCancel.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.textForgotPin.setOnClickListener {
            val action = LoginPinFragmentDirections
                .actionLoginPinFragmentToLoginFragment()
            findNavController().navigate(action)
        }
    }

    private fun setupAutoMove(container: ViewGroup) {
        val boxes = (0 until container.childCount)
            .map { container.getChildAt(it) as EditText }

        for (i in boxes.indices) {
            boxes[i].addTextChangedListener {
                if (it?.length == 1 && i < boxes.lastIndex) {
                    boxes[i + 1].requestFocus()
                }
            }
            boxes[i].setOnKeyListener { v, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (boxes[i].text.isEmpty() && i > 0) {
                        boxes[i - 1].apply {
                            requestFocus()
                            setSelection(length())
                        }
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        }
    }

    private fun getPinFromBoxes(container: ViewGroup): String {
        return (0 until container.childCount)
            .joinToString("") { (container.getChildAt(it) as EditText).text.toString() }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
