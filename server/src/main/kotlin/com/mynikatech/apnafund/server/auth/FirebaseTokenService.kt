package com.mynikatech.apnafund.server.auth

import com.google.firebase.auth.FirebaseAuth

object FirebaseTokenService {

    fun generateFirebaseCustomToken(
        userId: Int,
        email: String,
        groupId: Int?
    ): String {

        val uid = "user_$userId" // 🔑 STABLE Firebase UID

        val claims = mutableMapOf<String, Any>(
            "userId" to userId,
            "email" to email
        )

        groupId?.let {
            claims["groupId"] = it
        }

        return FirebaseAuth.getInstance()
            .createCustomToken(uid, claims)
    }
}