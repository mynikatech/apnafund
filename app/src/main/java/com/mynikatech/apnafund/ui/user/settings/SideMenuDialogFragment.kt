package com.mynikatech.apnafund.ui.user.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.session.PreferencesHelper
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.SplashActivity
import com.mynikatech.apnafund.ui.viewmodel.AdminViewModel
import com.mynikatech.apnafund.ui.viewmodel.ProfileSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserSummaryViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.ThemeManager
import com.mynikatech.apnafund.util.UserInputValidator
import com.mynikatech.apnafund.util.Converters
import kotlinx.coroutines.launch

class SideMenuDialogFragment : DialogFragment() {

    private val userViewModel: UserViewModel by viewModels()

    private val userSummaryViewModel: UserSummaryViewModel by viewModels()

    private val profileSharedViewModel: ProfileSharedViewModel by activityViewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.SideMenuDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_side_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navView = view.findViewById<NavigationView>(R.id.side_navigation_view)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_PIN -> {
                    // navigate to SET PIN fragment
                    val action = SideMenuDialogFragmentDirections
                        .actionSideMenuDialogFragmentToSetPinFragment(SessionManager.userId)
                    findNavController().navigate(action)
                }

                R.id.menu_theme -> {
                    // to toggle theme from day to night and vice versa
                    ThemeManager.toggleTheme(requireContext())
                    Snackbar.make(view, "Theme updated", Snackbar.LENGTH_SHORT).show()
                }

                R.id.menu_profile -> {
                    val dialogView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.dialog_edit_profile, null)

                    lifecycleScope.launch {
                        // Load the latest user (local cache). If you also want remote, call refresh first.
                        val user = userViewModel.fetchUser(SessionManager.userId)

                        val firstNameEdit = dialogView.findViewById<EditText>(R.id.editTextFirstName)
                        val lastNameEdit  = dialogView.findViewById<EditText>(R.id.editTextLastName)
                        val emailEdit     = dialogView.findViewById<EditText>(R.id.editTextEmail)
                        val phoneEdit     = dialogView.findViewById<EditText>(R.id.editTextPhone)

                        // Pre-fill from fetched user when available, else SessionManager
                        firstNameEdit.setText(user?.firstName ?: SessionManager.firstName)
                        lastNameEdit.setText(user?.lastName ?: SessionManager.lastName)
                        emailEdit.setText(user?.emailId ?: SessionManager.emailId)
                        phoneEdit.setText(user?.phoneNumber ?: SessionManager.phoneNumber)

                        val dialog = AlertDialog.Builder(requireContext())
                            .setTitle("Edit Profile")
                            .setView(dialogView)
                            .setPositiveButton("Save", null) // we override click
                            .setNegativeButton("Cancel", null)
                            .create()

                        dialog.setOnShowListener {
                            val saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

                            saveBtn.setOnClickListener {
                                // Run suspending work in a coroutine tied to the Fragment lifecycle
                                lifecycleScope.launch {
                                    val firstName = firstNameEdit.text.toString().trim()
                                    val lastName  = lastNameEdit.text.toString().trim()
                                    val email     = emailEdit.text.toString().trim().lowercase()
                                    val phone     = phoneEdit.text.toString().trim()

                                    // 1) Basic validation
                                    var isValid = true
                                    if (!UserInputValidator.isFirstNameValid(firstName)) {
                                        firstNameEdit.error = ApnaBankConstants.FIRST_NAME_ERROR_MESSAGE
                                        isValid = false
                                    }
                                    if (!UserInputValidator.isEmailValid(email)) {
                                        emailEdit.error = ApnaBankConstants.INVALID_EMAIL_ERROR_MESSAGE
                                        isValid = false
                                    }
                                    if (!UserInputValidator.isPhoneValid(phone)) {
                                        phoneEdit.error = ApnaBankConstants.INVALID_PHONE_ERROR_MESSAGE
                                        isValid = false
                                    }
                                    if (!isValid) return@launch

                                    // 2) Optional: uniqueness check (exclude current userId)
                                    val duplicate = userViewModel.isDuplicate(email, phone, SessionManager.userId)
                                    if (duplicate) {
                                        Toast.makeText(requireContext(), "Email or phone already in use.", Toast.LENGTH_LONG).show()
                                        return@launch
                                    }

                                    // 3) Build updated user (prefer copy from existing to preserve fields)
                                    val base = user ?: Users(
                                        userId = SessionManager.userId,
                                        firstName = SessionManager.firstName ?: "",
                                        lastName = SessionManager.lastName,
                                        emailId = SessionManager.emailId,
                                        phoneNumber = SessionManager.phoneNumber,
                                        isPinSet = SessionManager.isPinSet,
                                        status = "ACTIVE",
                                        userCode = Converters.generateUserCode(firstName, lastName)
                                    )
                                    val updatedUser = base.copy(
                                        firstName = firstName,
                                        lastName  = lastName,
                                        emailId   = email,
                                        phoneNumber = phone
                                    )

                                    // 4) Disable button while saving
                                    saveBtn.isEnabled = false

                                    try {
                                        // Remote-first save (suspend). Your saveOrUpdateUser is suspend now.
                                        val updatedId = userViewModel.saveOrUpdateUser(updatedUser)

                                        // 5) Update SessionManager after success
                                        SessionManager.userId = updatedId
                                        SessionManager.firstName = firstName
                                        SessionManager.lastName = lastName
                                        SessionManager.emailId = email
                                        SessionManager.phoneNumber = phone
                                        SessionManager.userName = "${SessionManager.firstName} ${SessionManager.lastName}".trim()

                                        profileSharedViewModel.publishDisplayName(SessionManager.userName)
                                        userSummaryViewModel.userName.postValue(SessionManager.userName)
                                        //userViewModel.refreshUser(SessionManager.userId)
                                        findNavController().previousBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("profile_updated", true)

                                        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                    } catch (t: Throwable) {
                                        // surface the error
                                        Toast.makeText(requireContext(), "Failed to update: ${t.message}", Toast.LENGTH_LONG).show()
                                        saveBtn.isEnabled = true
                                    }
                                }
                            }
                        }

                        dialog.show()
                    }
                }

                R.id.menu_feedback -> {
                    showFeedbackDialog()
                }

                R.id.menu_share -> {
                    Toast.makeText(requireContext(), "Share App", Toast.LENGTH_SHORT).show()
                }

                R.id.menu_logout -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Confirm Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes") { _, _ ->
                            val prefsHelper = PreferencesHelper(requireContext())
                            prefsHelper.clearSession()
                            Toast.makeText(
                                requireContext(),
                                "Successfully Logged Out",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(requireContext(), SplashActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            val action = SideMenuDialogFragmentDirections
                                .actionSideMenuDialogFragmentToLogInFragment()
                            findNavController().navigate(action)
                            dismiss()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            dismiss()
            true
        }
    }

    private fun showFeedbackDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Submit Feedback")

        val input = EditText(requireContext())
        input.hint = "Enter your feedback..."
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        input.minLines = 3
        builder.setView(input)

        builder.setPositiveButton("Submit") { dialog, _ ->
            val message = input.text.toString().trim()
            if (message.isNotEmpty()) {
                val adminViewModel: AdminViewModel by viewModels()
                lifecycleScope.launch {
                    adminViewModel.submitFeedback(SessionManager.userId, message)
                }
                Toast.makeText(requireContext(), "Thank you for your feedback!", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Please enter some feedback", Toast.LENGTH_SHORT)
                    .show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }
}