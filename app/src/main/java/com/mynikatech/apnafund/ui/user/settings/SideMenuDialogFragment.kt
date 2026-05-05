package com.mynikatech.apnafund.ui.user.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.net.dto.UserSaveSource
import com.mynikatech.apnafund.session.PreferencesHelper
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.SplashActivity
import com.mynikatech.apnafund.ui.auth.FirebaseAuthHelper
import com.mynikatech.apnafund.ui.viewmodel.AdminViewModel
import com.mynikatech.apnafund.ui.viewmodel.ProfileSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserSummaryViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.ThemeManager
import com.mynikatech.apnafund.util.UserInputValidator
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

                R.id.menu_password -> {
                    // navigate to reset password fragment
                    val action = SideMenuDialogFragmentDirections
                        .actionSideMenuDialogFragmentToResetPasswordFragment(SessionManager.userId)
                    findNavController().navigate(action)
                }

                R.id.menu_theme -> {
                    // to toggle theme from day to night and vice versa
                    ThemeManager.toggleTheme(requireContext())
                    Snackbar.make(view,
                        getString(R.string.message_theme_updated), Snackbar.LENGTH_SHORT).show()
                }

                R.id.menu_profile -> {
                    val dialogView = layoutInflater
                        .inflate(R.layout.dialog_edit_profile, null)

                    lifecycleScope.launch {
                        // Load the latest user (local cache). If you also want remote, call refresh first.
                        val user = userViewModel.fetchUser(SessionManager.userId)

                        val firstNameEdit =
                            dialogView.findViewById<EditText>(R.id.editTextFirstName)
                        val lastNameEdit = dialogView.findViewById<EditText>(R.id.editTextLastName)
                        val emailEdit = dialogView.findViewById<EditText>(R.id.editTextEmail)
                        val phoneEdit = dialogView.findViewById<EditText>(R.id.editTextPhone)

                        // Pre-fill from fetched user when available, else SessionManager
                        firstNameEdit.setText(user?.firstName ?: SessionManager.firstName)
                        lastNameEdit.setText(user?.lastName ?: SessionManager.lastName)
                        emailEdit.setText(user?.emailId ?: SessionManager.emailId)
                        phoneEdit.setText(user?.phoneNumber ?: SessionManager.phoneNumber)
                        emailEdit.isEnabled = false
                        phoneEdit.isEnabled = false

                        val dialog = AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.title_edit_profile))
                            .setView(dialogView)
                            .setPositiveButton(getString(R.string.text_save), null) // we override click
                            .setNegativeButton(getString(R.string.text_cancel_button), null)
                            .create()

                        dialog.setOnShowListener {
                            val saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

                            saveBtn.setOnClickListener {
                                // Run suspending work in a coroutine tied to the Fragment lifecycle
                                lifecycleScope.launch {
                                    val firstName = firstNameEdit.text.toString().trim()
                                    val lastName = lastNameEdit.text.toString().trim()
                                    val email = emailEdit.text.toString().trim().lowercase()
                                    val phone = phoneEdit.text.toString().trim()

                                    // 1) Basic validation
                                    var isValid = true
                                    if (!UserInputValidator.isFirstNameValid(firstName)) {
                                        firstNameEdit.error =
                                            ApnaBankConstants.FIRST_NAME_ERROR_MESSAGE
                                        isValid = false
                                    }
                                    if (!UserInputValidator.isEmailValid(email)) {
                                        emailEdit.error =
                                            ApnaBankConstants.INVALID_EMAIL_ERROR_MESSAGE
                                        isValid = false
                                    }
                                    if (!UserInputValidator.isPhoneValid(phone)) {
                                        phoneEdit.error =
                                            ApnaBankConstants.INVALID_PHONE_ERROR_MESSAGE
                                        isValid = false
                                    }
                                    if (!isValid) return@launch

                                    // 2) Optional: uniqueness check (exclude current userId)
                                    val duplicate = userViewModel.isDuplicate(
                                        email,
                                        phone,
                                        SessionManager.userId
                                    )
                                    if (duplicate) {
                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.error_email_phone_in_use),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@launch
                                    }

                                    // 3) Build updated user (prefer copy from existing to preserve fields)
                                    val now = System.currentTimeMillis()

                                    if (user == null) {
                                        showToast(getString(R.string.error_user_not_found_re_login))
                                        return@launch
                                    }

                                    val updatedUser = user.copy(
                                        firstName = firstName,
                                        lastName = lastName,
                                        emailId = email,
                                        phoneNumber = phone,
                                        updatedByUserId = SessionManager.userId,
                                        updatedAt = now
                                    )

                                    // 4) Disable button while saving
                                    saveBtn.isEnabled = false

                                    try {
                                        saveBtn.isEnabled = false

                                        val result = userViewModel.saveOrUpdateUser(
                                            updatedUser,
                                            userSaveSource = UserSaveSource.SELF_UPDATE
                                        )

                                        result
                                            .onSuccess { resp ->
                                                // Safe access
                                                SessionManager.userId = resp.userId
                                                SessionManager.firstName = firstName
                                                SessionManager.lastName = lastName
                                                SessionManager.emailId = email
                                                SessionManager.phoneNumber = phone
                                                SessionManager.userName =
                                                    "${SessionManager.firstName} ${SessionManager.lastName}".trim()

                                                /*profileSharedViewModel.publishDisplayName(
                                                    SessionManager.userName
                                                )*/
                                                userSummaryViewModel.userName.postValue(
                                                    SessionManager.userName
                                                )

                                                findNavController().previousBackStackEntry
                                                    ?.savedStateHandle
                                                    ?.set("profile_updated", true)

                                                Toast.makeText(
                                                    requireContext(),
                                                    getString(R.string.message_profile_updated_success),
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                dialog.dismiss()
                                            }
                                            .onFailure { e ->
                                                Log.e(
                                                    "ProfileUpdate",
                                                    "Failed to update profile",
                                                    e
                                                )

                                                Toast.makeText(
                                                    requireContext(),
                                                    getString(R.string.error_server),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }

                                        saveBtn.isEnabled = true
                                    } catch (t: Throwable) {
                                        // ONLY for unexpected crashes (not API errors)
                                        Log.e("ProfileUpdate", "Unexpected error", t)

                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.error_server),
                                            Toast.LENGTH_LONG
                                        ).show()

                                    } finally {
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
                    Toast.makeText(requireContext(),
                        getString(R.string.message_share_app), Toast.LENGTH_SHORT).show()
                }

                R.id.menu_logout -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.title_confirm_logout))
                        .setMessage(getString(R.string.message_confirm_logout))
                        .setPositiveButton(getString(R.string.text_yes)) { _, _ ->
                            try {
                                FirebaseFirestore.getInstance().terminate()
                            } catch (e: Exception) {
                                Log.w("LOGOUT", "Firestore terminate failed", e)
                            }

                            FirebaseAuthHelper.signOut()
                            val prefsHelper = PreferencesHelper(requireContext())
                            prefsHelper.clearSession()
                            SessionManager.clearSession()
                            FirebaseFirestore.getInstance().clearPersistence()
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.message_successful_log_out),
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(requireContext(), SplashActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            val action = SideMenuDialogFragmentDirections
                                .actionSideMenuDialogFragmentToLogInFragment()
                            findNavController().navigate(action)
                            dismiss()
                        }
                        .setNegativeButton(getString(R.string.text_cancel_button), null)
                        .show()
                }
            }
            true
        }
    }

    private fun showFeedbackDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.title_submit_feedback))

        val input = EditText(requireContext())
        input.hint = getString(R.string.hint_enter_feedback)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        input.minLines = 3
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.button_submit)) { dialog, _ ->
            val message = input.text.toString().trim()
            if (message.isNotEmpty()) {
                val adminViewModel: AdminViewModel by viewModels()
                lifecycleScope.launch {
                    adminViewModel.submitFeedback(SessionManager.userId, message)
                }
                Toast.makeText(requireContext(),
                    getString(R.string.message_thank_feedback), Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(requireContext(),
                    getString(R.string.message_enter_feedback), Toast.LENGTH_SHORT)
                    .show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton(getString(R.string.text_cancel_button)) { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showToast(
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        Toast.makeText(requireContext(), message, duration).show()
    }
}