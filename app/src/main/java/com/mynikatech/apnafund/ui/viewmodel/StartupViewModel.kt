package com.mynikatech.apnafund.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.mynikatech.apnafund.ApnaFundApplication

class StartupViewModel(
) : ViewModel() {

    private val userRolesRepository = ApnaFundApplication.userRolesRepository

    suspend fun restoreFirebaseSession(email: String) {

        Log.d("Firebase", "Calling restoreFirebaseSession")

        val response = userRolesRepository.getUserByEmail(email)

        if (null == response){
            Log.w("Firebase", "Firebase UID not available — skipping restore")
            return
        }

        val token = response.firebaseToken

        if (token.isNullOrBlank()) {
            Log.w("Firebase", "Firebase token not available — skipping restore")
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