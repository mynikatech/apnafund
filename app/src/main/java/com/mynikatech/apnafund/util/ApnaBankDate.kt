package com.mynikatech.apnafund.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ApnaBankDate {
    companion object {

        private val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        fun getMonthNameToNumberMap(): Map<String, Int> {
            return months.withIndex().associate { it.value to (it.index + 1) }
        }

        fun getCurrentDate(): String {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return dateFormat.format(Date())
        }

        fun parseDate(dateStr: String, pattern: String = "dd/MM/yyyy"): Date {

            val formatter = SimpleDateFormat(pattern, Locale.getDefault())
            return formatter.parse(dateStr)
        }

        fun getDaysBetween(startDate: Date, endDate: Date): Long {

            val diffInMillis = endDate.time - startDate.time

            return TimeUnit.MILLISECONDS.toDays(diffInMillis) - 1
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getMonthsBetween(startDateStr: String, endDateStr: String): Int {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val startDate = LocalDate.parse(startDateStr, formatter)
            val endDate = LocalDate.parse(endDateStr, formatter)

            val period = Period.between(startDate, endDate)

            return period.years * 12 + period.months
        }

        private fun createDate(day: Int, month: Int, year: Int): Date {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month - 1)  // Calendar months are 0-based
            calendar.set(Calendar.DAY_OF_MONTH, day)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.time
        }

        fun createDateString(
            day: Int,
            month: Int,
            year: Int,
            pattern: String = "dd/MM/yyyy"
        ): String {
            val date = createDate(day, month, year)
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            return sdf.format(date)
        }

        fun calculateMatDate(
            startDateStr: String,
            periodMonths: Double,
            dateFormat: String = "dd/MM/yyyy"
        ): String {
            val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
            val startDate: Date = sdf.parse(startDateStr) ?: return ""

            val calendar = Calendar.getInstance()
            calendar.time = startDate
            calendar.add(Calendar.MONTH, periodMonths.toInt())
            calendar.add(Calendar.DATE, -1)

            return sdf.format(calendar.time)
        }

        fun normalizeMonthYear(month: Int, year: Int): Pair<String, String> {
            val m = month.coerceIn(1, 12)
            val y = year.coerceIn(1900, 9999)
            return m.toString() to y.toString()
        }


    }
}