package com.mynikatech.apnafund.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mynikatech.apnafund.session.PreferencesHelper
import com.mynikatech.apnafund.util.ThemeManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefsHelper = PreferencesHelper(this)
        Log.d("Sunil", "SplashActivity Called")
        Log.d("Sunil", "User Logged in : ${prefsHelper.isLoggedIn()}")
        Log.d("Sunil", "Is PIN set : ${prefsHelper.isPinSet()}")
        val startDest = when {
            prefsHelper.isLoggedIn() && prefsHelper.isPinSet() -> "enter_pin"
            prefsHelper.isLoggedIn() -> "home"
            else -> "login"
        }
        val userId = prefsHelper.getUserId()
        Log.d("Sunil", "User Id is: $userId")

        //Amplify.configure(getApplicationContext())
        val intent = Intent(this, MainActivity::class.java)
        ThemeManager.applySavedTheme(this)
        intent.putExtra("start_dest", startDest)
        intent.putExtra("user_id", userId)
        startActivity(intent)
        finish()
    }


}