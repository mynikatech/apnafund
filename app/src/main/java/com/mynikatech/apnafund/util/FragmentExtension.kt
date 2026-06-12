package com.mynikatech.apnafund.util

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.mynikatech.apnafund.R

fun Fragment.showAlert(
    message: String,
    title: String = getString(R.string.title_alert)
) {

    if (!isAdded) return

    AlertDialog.Builder(requireContext())
        .setTitle(title)
        .setMessage(message)
        .setCancelable(false)
        .setPositiveButton(
            getString(R.string.text_button_ok),
            null
        )
        .show()
}

fun Fragment.showSuccessSnackbar(message: String) {
    Snackbar.make(
        requireView(),
        message,
        Snackbar.LENGTH_LONG
    ).show()
}