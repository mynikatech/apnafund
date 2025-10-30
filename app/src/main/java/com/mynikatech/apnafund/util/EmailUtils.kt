package com.mynikatech.apnafund.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object EmailUtils {
    fun sendEmail(to: String, subject: String, message: String, context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$to")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }
}