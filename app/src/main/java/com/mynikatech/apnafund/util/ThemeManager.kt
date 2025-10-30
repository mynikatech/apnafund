package com.mynikatech.apnafund.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    private const val PREFS_NAME = "user_prefs"
    private const val KEY_DARK_MODE = "dark_theme"

    fun applyTheme(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun toggleTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean(KEY_DARK_MODE, false)
        val newMode = if (isDark) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES

        prefs.edit().putBoolean(KEY_DARK_MODE, !isDark).apply()
        AppCompatDelegate.setDefaultNightMode(newMode)
    }

    fun applySavedTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean(KEY_DARK_MODE, false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentMode = prefs.getBoolean(KEY_DARK_MODE, false)
        return currentMode

    }
}