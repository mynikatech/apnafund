package com.mynikatech.apnafund.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.mynikatech.apnafund.ApnaFundApplication

class StartupViewModel(
) : ViewModel() {

    private val userRolesRepository = ApnaFundApplication.userRolesRepository

    suspend fun restoreFirebaseSession(
        email: String?,
        phoneNumber: String?
    ) {

        Log.d("Firebase", "Calling restoreFirebaseSession")

        val user = when {
            !email.isNullOrBlank() ->
                userRolesRepository.getUserByEmail(email)

            !phoneNumber.isNullOrBlank() ->
                userRolesRepository.getUserByPhone(phoneNumber)

            else -> {
                Log.w("Firebase", "Neither email nor phone available")
                return
            }
        }

        if (user == null) {
            Log.w("Firebase", "User not found")
            return
        }

        val token = user.firebaseToken

        if (token.isNullOrBlank()) {
            Log.w("Firebase", "Firebase token not available")
            return
        }

        FirebaseAuth.getInstance()
            .signInWithCustomToken(token)
            .addOnSuccessListener {
                Log.d("Firebase", "Firebase session restored")
            }
            .addOnFailureListener {
                Log.e("Firebase", "Firebase login failed", it)
            }
    }
}