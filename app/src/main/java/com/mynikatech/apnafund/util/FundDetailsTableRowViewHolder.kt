package com.mynikatech.apnafund.util

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.ImageView
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.mynikatech.apnafund.R
import com.google.android.material.color.MaterialColors

class FundDetailsTableRowViewHolder(context: Context, applyLoan: Boolean = false) {
    val tvNo = createTextView(context, Gravity.CENTER)
    val tvName = createTextView(context, Gravity.START)
    val tvDepAmt = createTextView(context, Gravity.END)
    val tvLoanAmt = createTextView(context, Gravity.END)
    val tvTotPendingAmt = createTextView(context, Gravity.END)
    val tvTotIntPaid = createTextView(context, Gravity.END)
    val tvExpMatAmt = createTextView(context, Gravity.END)
    val tvApplyLoan = createImageView(context, "Apply Loan", R.drawable.icon_loan)

    val row: TableRow = TableRow(context).apply {
        addView(tvNo)
        addView(tvName)
        addView(tvDepAmt)
        addView(tvLoanAmt)
        addView(tvTotPendingAmt)
        addView(tvTotIntPaid)
        addView(tvExpMatAmt)
        if(applyLoan)
            addView(tvApplyLoan)
    }

    companion object {
        fun createTextView(
            context: Context,
            gravity: Int
        ): TextView {

            return TextView(context).apply {

                setPadding(8, 8, 8, 8)

                this.gravity = gravity or Gravity.CENTER_VERTICAL

                textSize = 11f

                setTextColor(
                    MaterialColors.getColor(
                        this,
                        com.google.android.material.R.attr.colorOnSurface
                    )
                )

                setTypeface(typeface, Typeface.NORMAL)

            }
        }
        fun createImageView(context:Context, tooltip: String, drawableRes: Int): ImageView {
            return ImageView(context).apply {
                val sizeInPx = (19 * resources.displayMetrics.density).toInt()
                setImageResource(drawableRes)
                layoutParams = TableRow.LayoutParams(sizeInPx, sizeInPx)
                setPadding(8, 8, 8, 8)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                background = null
                ViewCompat.setTooltipText(this, tooltip)
            }

        }
    }
}