package com.mynikatech.apnafund.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mynikatech.apnafund.session.PreferencesHelper
import com.mynikatech.apnafund.util.ThemeManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefsHelper = PreferencesHelper(this)


        val userId = prefsHelper.getUserId()
        Log.d("Apnafund", "User Id is: $userId")
        Log.d("Apnafund", "Is PIN set : ${prefsHelper.isPinSet()}")
        val startDest = when {
            userId > 0 && prefsHelper.isPinSet() -> "enter_pin"
            userId > 0 -> "home"
            else -> "login"
        }
        val appLocale = LocaleListCompat.forLanguageTags(
            prefsHelper.getLanguage()
        )

        AppCompatDelegate.setApplicationLocales(appLocale)


        //Amplify.configure(getApplicationContext())
        val intent = Intent(this, MainActivity::class.java)
        ThemeManager.applySavedTheme(this)
        intent.putExtra("start_dest", startDest)
        intent.putExtra("user_id", userId)
        startActivity(intent)
        finish()
    }


}