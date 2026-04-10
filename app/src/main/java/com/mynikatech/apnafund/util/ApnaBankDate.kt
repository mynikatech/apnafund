package com.mynikatech.apnafund.util


import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ApnaBankDate {
    companion object {

        private const val DATE_PATTERN = "dd/MM/yyyy"

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

        fun getMonthsBetween(
            startDateStr: String,
            endDateStr: String,
            pattern: String = DATE_PATTERN
        ): Int {
            val sdf = SimpleDateFormat(pattern, Locale.US)

            val startDate = sdf.parse(startDateStr) ?: return 0
            val endDate = sdf.parse(endDateStr) ?: return 0

            return getMonthsBetween(startDate, endDate)
        }

        fun getMonthsBetween(start: Date, end: Date): Int {
            val startCal = Calendar.getInstance().apply { time = start }
            val endCal = Calendar.getInstance().apply { time = end }

            var months =
                (endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12 +
                        (endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH))

            // Adjust if end day is before start day
            if (endCal.get(Calendar.DAY_OF_MONTH) < startCal.get(Calendar.DAY_OF_MONTH)) {
                months -= 1
            }

            return months
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

        fun todayKey(): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getDefault()
            return sdf.format(java.util.Date())
        }

        fun toDateLabel(ts: Timestamp?): String {
            if (ts == null) return ""

            val msgDate = ts.toDate()
            return when {
                isToday(msgDate) -> "Today"
                isYesterday(msgDate) -> "Yesterday"
                else -> SimpleDateFormat("dd MMM yyyy", Locale.US).format(msgDate)
            }
        }

        private fun isToday(date: Date): Boolean {
            val today = Calendar.getInstance()
            val cal = Calendar.getInstance().apply { time = date }

            return today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    today.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
        }

        private fun isYesterday(date: Date): Boolean {
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            val cal = Calendar.getInstance().apply { time = date }

            return yesterday.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    yesterday.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
        }

        fun toRelativeTime(date: Date?): String {
            if (date == null) return ""

            val now = System.currentTimeMillis()
            val diff = now - date.time

            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "$minutes min${if (minutes > 1) "s" else ""} ago"
                hours < 24 -> "$hours hr${if (hours > 1) "s" else ""} ago"
                days == 1L -> "Yesterday"
                days < 7 -> "$days days ago"
                else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
            }
        }

        fun toRelativeTimeFromIso(isoString: String?): String {
            if (isoString.isNullOrBlank()) return ""

            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US)
                val date = sdf.parse(isoString)
                toRelativeTime(date)
            } catch (e: Exception) {
                ""
            }
        }

        fun formatServerTimestamp(dateStr: String?): String {
            if (dateStr.isNullOrBlank()) return ""

            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSX", Locale.US)
                val date = inputFormat.parse(dateStr)

                val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                outputFormat.format(date!!)
            } catch (e: Exception) {
                ""
            }
        }

        fun compareDates(date1: String, date2: String, pattern: String): Int {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())

            val d1 = sdf.parse(date1)
            val d2 = sdf.parse(date2)

            return d1.compareTo(d2)
        }
    }


}