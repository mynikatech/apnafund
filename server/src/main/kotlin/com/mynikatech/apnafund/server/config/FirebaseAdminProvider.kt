package com.mynikatech.apnafund.server.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

object FirebaseAdminProvider {

    fun init() {
        if (FirebaseApp.getApps().isNotEmpty()) return

        val credPath = System.getenv("FIREBASE_CRED_PATH")
            ?: throw IllegalStateException("FIREBASE_CRED_PATH not set")

        val serviceAccount = FileInputStream(credPath)

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        FirebaseApp.initializeApp(options)
    }
}