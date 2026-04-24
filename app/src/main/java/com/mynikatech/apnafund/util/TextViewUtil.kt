package com.mynikatech.apnafund.util

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.mynikatech.apnafund.R

fun TextView.applyStatusStyle(status: String) {

    when (status.uppercase()) {

        "ACTIVE", "APPROVED" -> {
            setBackgroundResource(R.drawable.status_chip_active)
            setTextColor(ContextCompat.getColor(context, R.color.status_approved))
        }

        "PENDING", "PENDING_APPROVAL" -> {
            setBackgroundResource(R.drawable.status_chip_pending)
            setTextColor(ContextCompat.getColor(context, R.color.status_pending))
        }

        "REJECTED" -> {
            setBackgroundResource(R.drawable.status_chip_rejected)
            setTextColor(ContextCompat.getColor(context, R.color.status_rejected))
        }

        "CLOSED" -> {
            setBackgroundResource(R.drawable.status_chip_closed)
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }

        else -> {
            setBackgroundResource(R.drawable.status_chip_background) // neutral
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
        }
    }
}