package com.mynikatech.apnafund.util

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.mynikatech.apnafund.R

fun TextView.applyStatusStyle(status: String) {

    val context = this.context

    when (status.uppercase()) {

        "ACTIVE", "APPROVED" -> {
            setBackgroundResource(R.drawable.status_chip_active)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimaryContainer))
        }

        "PENDING", "PENDING_APPROVAL" -> {
            setBackgroundResource(R.drawable.status_chip_pending)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnTertiaryContainer))
        }

        "REJECTED" -> {
            setBackgroundResource(R.drawable.status_chip_rejected)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnErrorContainer))
        }

        "CLOSED" -> {
            setBackgroundResource(R.drawable.status_chip_closed)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant))
        }

        else -> {
            setBackgroundResource(R.drawable.status_chip_background)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
        }
    }
}