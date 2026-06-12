package com.mynikatech.apnafund.ui.auth

import android.os.Bundle
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
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.FragmentSetPinBinding
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch

class SetPinFragment : Fragment() {

    private lateinit var binding: FragmentSetPinBinding
    private var userId: Int = -1
    private val userViewModel: UserViewModel by viewModels()
    private var isRegistrationFlow: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetPinBinding.inflate(inflater, container, false)
        arguments?.let {
            userId = it.getInt("userId")
            isRegistrationFlow = it.getBoolean("isRegistrationFlow", false)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAutoMove(binding.entryPinBoxes)
        setupAutoMove(binding.confirmPinBoxes)

        binding.buttonSubmit.setOnClickListener {
            binding.buttonSubmit.isEnabled = false
            val pin = getPinFromBoxes(binding.entryPinBoxes)
            val confirmPin = getPinFromBoxes(binding.confirmPinBoxes)

            if (pin.length != 4 || confirmPin.length != 4) {
                showToast(getString(R.string.message_enter_4_digit_pin))
                binding.buttonSubmit.isEnabled = true
                return@setOnClickListener
            }

            if (pin != confirmPin) {
                showToast(getString(R.string.error_pin_not_match))
                binding.buttonSubmit.isEnabled = true
                return@setOnClickListener
            }

            // Save PIN securely
            lifecycleScope.launch {
                if (userViewModel.isPINReused(userId, pin)) {
                    showToast(getString(R.string.error_pin_not_same_last_3))
                    binding.buttonSubmit.isEnabled = true
                    return@launch
                }
                userViewModel.changeUserPIN(userId, pin)
                showToast(getString(R.string.message_pin_set_success))
                if (isRegistrationFlow) {
                    val action =
                        SetPinFragmentDirections
                            .actionSetPinFragmentToLoginPinFragment(
                                userId
                            )
                    findNavController().navigate(action)
                } else {

                    findNavController().navigateUp()
                }
            }
        }
        binding.buttonCancel.setOnClickListener {

            if (isRegistrationFlow) {
                findNavController().navigate(
                    R.id.action_setPinFragment_to_loginFragment
                )
            } else {
                findNavController().navigateUp()
            }
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
