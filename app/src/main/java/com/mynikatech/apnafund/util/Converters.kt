package com.mynikatech.apnafund.util

import android.util.Base64
import androidx.room.TypeConverter
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.data.model.FundMembers
import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.net.dto.UserFundMembership
import com.mynikatech.apnafund.net.dto.UserGroupMembership
import com.mynikatech.apnafund.session.SessionManager
import java.security.MessageDigest
import java.util.Date

object Converters {

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }

    fun formatCurrency(value: Double): String {
        return buildString {
            append("₹")
            append(String.format("%,.0f", value)) // use NumberFormat if needed
        } // use NumberFormat if needed
    }

    fun generateUserCode(firstName: String, lastName: String?): String {
        val firstPart = firstName.take(2).padEnd(2, 'X')
        val lastPart = lastName?.take(2)?.padEnd(2, 'X') ?: "XX"
        val randomNumber = (0..999999).random().toString().padStart(6, '0')
        return (firstPart + lastPart + randomNumber).uppercase()
    }

    fun generateFundCode(fundName: String): String {
        val fundNameWithoutSpace = fundName.filter { !it.isWhitespace() }
        val firstPart = fundNameWithoutSpace.take(4).padEnd(4, 'X')
        val randomNumber = (0..999999).random().toString().padStart(6, '0')
        return (firstPart + randomNumber).uppercase()
    }

    fun generateGroupCode(groupName: String): String {
        val groupNameWithoutSpace = groupName.filter { !it.isWhitespace() }
        val firstPart = groupNameWithoutSpace.take(4).padEnd(4, 'X')
        val randomNumber = (0..999999).random().toString().padStart(6, '0')
        return (firstPart + randomNumber).uppercase()
    }

    fun userHasPrivilege(privilegeCode: String): Boolean {

        val roleIds = SessionManager.roleIds
        roleIds.forEach { roleId ->
            val privileges = ApnaFundApplication.rolePrivilegesMap[roleId]
            if (privileges != null && privilegeCode in privileges) {
                return true
            }
        }
        return false
    }

    fun String.toTitleCase(): String {
        return this.trim()
            .lowercase()
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    fun getFormattedPeriodDate(years: Double, months: Double): String {
        val formattedPeriod: String = when {
            years > 1 && months > 0 -> "$years yrs $months mos"
            years > 1 -> "$years yrs"
            years == 1.0 && months == 0.0 -> "$years yr"
            else -> "$months mos"
        }
        return formattedPeriod
    }

    fun hashPassword(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(inputPassword: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrBlank()) return false
        val inputHash = hashPassword(inputPassword)
        return inputHash == storedHash
    }

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val salt = "apnaBankFixedSalt" // You may later improve this with per-user salt
        val bytes = digest.digest((pin + salt).toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun formatUserName(firstName: String, lastName: String?): String {
        return "${firstName} ${lastName}"
    }

    fun formatWithBraces(name: String): String {
        //add braces
        return "($name)"
    }

    fun formatLoanPeriod(totalMonths: Int): String {
        if (totalMonths <= 0) return "0 m"

        val years = totalMonths / 12
        val months = totalMonths % 12

        return when {
            years >= 1 && months > 0 -> "$years y $months m"
            years >= 1 -> "$years y"
            else -> "$months m"
        }
    }

    fun toDisplayRole(role: String): String {
        return role
            .lowercase()
            .split("_")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    fun canManageFunds(member: GroupMembers?): Boolean {
        return member?.status == "ACTIVE" &&
                member.role in listOf("PRIMARY_MODERATOR", "MODERATOR")
    }

    private fun hasModeratorAccess(
        memberships: List<out Any>?,
        roleSelector: (Any) -> String,
        statusSelector: (Any) -> String
    ): Boolean {
        return memberships?.any {
            statusSelector(it) == "ACTIVE" &&
                    roleSelector(it) in listOf("PRIMARY_MODERATOR", "MODERATOR")
        } == true
    }

    fun canAccessAdmin(): Boolean {

        // 1) Global ADMIN
        if (SessionManager.roleNames?.contains("ADMIN") == true) return true

        // 2) Group moderator access
        val groupAccess = hasModeratorAccess(
            SessionManager.userGroupMemberships,
            roleSelector = { (it as UserGroupMembership).role },
            statusSelector = { (it as UserGroupMembership).status }
        )

        if (groupAccess) return true

        // 3) Fund moderator access
        val fundAccess = hasModeratorAccess(
            SessionManager.userFundMemberships,
            roleSelector = { (it as UserFundMembership).role },
            statusSelector = { (it as UserFundMembership).status }
        )

        return fundAccess
    }

    fun canAddFundMembers(fundId: Int): Boolean {

        // 1) Global Admin
        if (SessionManager.roleNames?.contains("ADMIN") == true) return true

        // 2) Fund moderator
        val fundAccess = SessionManager.userFundMemberships?.any {
            it.fundId == fundId &&
                    it.status == "ACTIVE" &&
                    (it.role == "PRIMARY_MODERATOR" || it.role == "MODERATOR")
        } == true

        if (fundAccess) return true
        else return false


    }

}