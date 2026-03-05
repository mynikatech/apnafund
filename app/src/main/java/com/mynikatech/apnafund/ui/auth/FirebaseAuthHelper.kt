package com.mynikatech.apnafund.ui.auth

import com.google.firebase.auth.FirebaseAuth
import com.mynikatech.apnafund.session.SessionManager
import android.util.Log

object FirebaseAuthHelper {

    fun ensureFirebaseSignedIn(
        firebaseToken: String,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()

        // Already signed in → nothing to do
        if (auth.currentUser != null) {
            SessionManager.firebaseUid = auth.currentUser!!.uid
            onSuccess()
            return
        }

        auth.signInWithCustomToken(firebaseToken)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid.orEmpty()
                SessionManager.firebaseUid = uid

                Log.d("FirebaseAuth", "Signed in with custom token, uid=$uid")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseAuth", "Firebase custom auth failed", e)
                onFailure(e)
            }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        SessionManager.firebaseUid = ""
        SessionManager.firebaseToken = null
    }
}
