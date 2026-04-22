package com.mynikatech.apnafund.ui.help

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HelpItem(
    val id: String,
    val titleResId: Int,
    val descriptionResId: Int,
    val videoUrl: String? = null,
    val deepLink: Int? = null // nav graph destination id
): Parcelable