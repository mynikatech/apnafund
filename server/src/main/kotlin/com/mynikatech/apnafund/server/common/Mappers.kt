package com.mynikatech.apnafund.server.common

import com.mynikatech.apnafund.net.dto.DepositsDto
import com.mynikatech.apnafund.net.dto.FeedbackDto
import com.mynikatech.apnafund.net.dto.FundDetailsDto
import com.mynikatech.apnafund.net.dto.FundMembersDto
import com.mynikatech.apnafund.net.dto.FundsDto
import com.mynikatech.apnafund.net.dto.GroupMemberWithNameDto
import com.mynikatech.apnafund.net.dto.GroupMembersDto
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.LoanDetailsDto
import com.mynikatech.apnafund.net.dto.LoanEmisDto
import com.mynikatech.apnafund.net.dto.LoansDto
import com.mynikatech.apnafund.net.dto.PrivilegeDto
import com.mynikatech.apnafund.net.dto.RolePrivilegeDto
import com.mynikatech.apnafund.net.dto.RolesDto
import com.mynikatech.apnafund.net.dto.TypeDto
import com.mynikatech.apnafund.net.dto.UserNotificationsDto
import com.mynikatech.apnafund.net.dto.UserPasswordHistoryDto
import com.mynikatech.apnafund.net.dto.UserPinHistoryDto
import com.mynikatech.apnafund.net.dto.UserRolesDto
import com.mynikatech.apnafund.net.dto.UserWithGroupDto
import com.mynikatech.apnafund.net.dto.UsersDto
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

/* ---------- Helper getters for nullable primitives ---------- */
private fun ResultSet.getNullableInt(column: String): Int? =
    getObject(column)?.let { getInt(column) }

private fun ResultSet.getNullableLong(column: String): Long? =
    getObject(column)?.let { getLong(column) }

private fun ResultSet.getNullableBoolean(column: String): Boolean? =
    getObject(column)?.let { getBoolean(column) }

private fun ResultSet.getNullableDateString(column: String): String? =
    getDate(column)?.toString()

/* ============================================================
 * USERS
 * ============================================================ */
val UsersRowMapper = RowMapper<UsersDto> { rs: ResultSet, _: StatementContext ->
    UsersDto(
        userId = rs.getInt("userId"),
        firstName = rs.getString("firstName"),
        lastName = rs.getString("lastName"),
        emailId = rs.getString("emailId"),
        phoneNumber = rs.getString("phoneNumber"),
        status = rs.getString("status"),
        passwordHash = rs.getString("passwordHash"),
        createdDate = rs.getString("createdDate"),
        isPinSet = rs.getBoolean("isPinSet"),
        hashPIN = rs.getString("hashPIN"),
        firebaseUserId = rs.getString("firebaseUserId"),
        userCode = rs.getString("userCode")
    )
}

/* ============================================================
 * GROUPS
 * ============================================================ */
val GroupsRowMapper = RowMapper<GroupsDto> { rs: ResultSet, _: StatementContext ->
    GroupsDto(
        groupId = rs.getInt("groupId"),
        groupName = rs.getString("groupName"),
        moderator = rs.getNullableInt("moderator"),
        createdDate = rs.getString("createdDate"),
        description = rs.getString("description"),
        groupCode = rs.getString("groupCode"),
        status = rs.getString("status")
    )
}

val GroupMembersRowMapper = RowMapper<GroupMembersDto> { rs, _ ->
    GroupMembersDto(
        groupMemberId = rs.getInt("groupMemberId"),
        userId = rs.getInt("userId"),
        groupId = rs.getInt("groupId"),
        joiningDate = rs.getDate("joiningDate").toString()
    )
}

val GroupMemberWithNameRowMapper = RowMapper<GroupMemberWithNameDto> { rs, _ ->
    GroupMemberWithNameDto(
        groupMemberId = rs.getInt("groupMemberId"),
        userId = rs.getInt("userId"),
        groupId = rs.getInt("groupId"),
        joiningDate = rs.getDate("joiningDate").toString(),
        firstName = rs.getString("firstName"),
        lastName = rs.getString("lastName") ?: "",
        emailId = rs.getString("emailId")
    )
}

/* ============================================================
 * ROLES & PRIVILEGES
 * ============================================================ */
val RolesRowMapper = RowMapper<RolesDto> { rs, _ ->
    RolesDto(
        roleId = rs.getInt("roleId"),
        roleCode = rs.getString("roleCode"),
        roleDescription = rs.getString("roleDescription"),
        status = rs.getString("status")
    )
}

val PrivilegeRowMapper = RowMapper<PrivilegeDto> { rs, _ ->
    PrivilegeDto(
        privilegeId = rs.getInt("privilegeId"),
        privilegeCode = rs.getString("privilegeCode"),
        privilegeDescription = rs.getString("privilegeDescription"),
        status = rs.getString("status")
    )
}

val UserRolesRowMapper = RowMapper<UserRolesDto> { rs, _ ->
    UserRolesDto(
        userRoleId = rs.getInt("userRoleId"),
        userId = rs.getInt("userId"),
        roleId = rs.getInt("roleId"),
        status = rs.getString("status")
    )
}

val RolePrivilegeRowMapper = RowMapper<RolePrivilegeDto> { rs, _ ->
    RolePrivilegeDto(
        rolePrivilegeId = rs.getInt("rolePrivilegeId"),
        privilegeId = rs.getInt("privilegeId"),
        roleId = rs.getInt("roleId"),
        status = rs.getString("status")
    )
}

/* ============================================================
 * FUNDS
 * ============================================================ */
val FundsRowMapper = RowMapper<FundsDto> { rs, _ ->
    FundsDto(
        fundId = rs.getInt("fundId"),
        fundName = rs.getString("fundName"),
        fundStartDate = rs.getString("fundStartDate"),
        fundMaturityDate = rs.getString("fundMaturityDate"),
        fundPeriod = rs.getDouble("fundPeriod"),
        depositionFrequency = rs.getString("depositionFrequency"),
        moderator = rs.getNullableInt("moderator") ?: 0,
        recurringDepositAmount = rs.getDouble("recurringDepositAmount"),
        fundStatus = rs.getString("fundStatus"),
        loanInterestRate = rs.getDouble("loanInterestRate"),
        lateFeeRate = rs.getDouble("lateFeeRate"),
        monthlyDepDateBy = rs.getInt("monthlyDepDateBy"),
        groupId = rs.getInt("groupId"),
        fundCode = rs.getString("fundCode")
    )
}

val FundMembersRowMapper = RowMapper<FundMembersDto> { rs, _ ->
    FundMembersDto(
        fundMemberId = rs.getInt("fundMemberId"),
        userId = rs.getInt("userId"),
        fundId = rs.getInt("fundId"),
        joiningDate = rs.getDate("joiningDate").toString()
    )
}

val FundDetailsRowMapper = RowMapper<FundDetailsDto> { rs, _ ->
    FundDetailsDto(
        fundDetailsId = rs.getInt("fundDetailsId"),
        fundId = rs.getInt("fundId"),
        totalExpectedDeposit = rs.getDouble("totalExpectedDeposit"),
        totalCurrentDeposit = rs.getDouble("totalCurrentDeposit"),
        totalCurrentLateFee = rs.getDouble("totalCurrentLateFee"),
        totalCurrentInterestCollected = rs.getDouble("totalCurrentInterestCollected"),
        totalExpectedMaturityAmount = rs.getDouble("totalExpectedMaturityAmount"),
        totalCurrAmount = rs.getDouble("totalCurrAmount")
    )
}

/* ============================================================
 * DEPOSITS
 * ============================================================ */
val DepositsRowMapper = RowMapper<DepositsDto> { rs, _ ->
    DepositsDto(
        depositId = rs.getInt("depositId"),
        depositorId = rs.getInt("depositorId"),
        depositedDate = rs.getString("depositedDate"),
        depositAmount = rs.getDouble("depositAmount"),
        depositMonth = rs.getString("depositMonth"),
        depositYear = rs.getString("depositYear"),
        fundId = rs.getInt("fundId"),
        lateFee = rs.getDouble("lateFee")
    )
}

/* ============================================================
 * LOANS
 * ============================================================ */
val LoansRowMapper = RowMapper<LoansDto> { rs, _ ->
    LoansDto(
        loanId = rs.getInt("loanId"),
        fundId = rs.getInt("fundId"),
        loanNumber = rs.getString("loanNumber"),
        borrowerId = rs.getInt("borrowerId"),
        issuedDate = rs.getString("issuedDate"),
        period = rs.getDouble("period"),
        loanAmount = rs.getDouble("loanAmount"),
        maturityDate = rs.getString("maturityDate"),
        rateOfInterest = rs.getDouble("rateOfInterest"),
        status = rs.getString("status"),
        workflowStatus = rs.getString("workflowStatus")
    )
}

val LoanDetailsRowMapper = RowMapper<LoanDetailsDto> { rs, _ ->
    LoanDetailsDto(
        loanDetailsId = rs.getInt("loanDetailsId"),
        loanId = rs.getInt("loanId"),
        origPrincipal = rs.getDouble("origPrincipal"),
        totalInterest = rs.getDouble("totalInterest"),
        totalAmount = rs.getDouble("totalAmount"),
        emiInterest = rs.getDouble("emiInterest"),
        currTotalIntPaid = rs.getDouble("currTotalIntPaid"),
        currPrincipal = rs.getDouble("currPrincipal")
    )
}

val LoanEmisRowMapper = RowMapper<LoanEmisDto> { rs, _ ->
    LoanEmisDto(
        loanEmiId = rs.getInt("loanEmiId"),
        loanId = rs.getInt("loanId"),
        emiMonth = rs.getString("emiMonth"),
        emiYear = rs.getString("emiYear"),
        emiDepositedDate = rs.getString("emiDepositedDate"),
        emiDepositedAmount = rs.getDouble("emiDepositedAmount"),
        prepaymentAmount = rs.getDouble("prepaymentAmount"),
        lateFee = rs.getDouble("lateFee")
    )
}

/* ============================================================
 * TYPES & NOTIFICATIONS
 * ============================================================ */
val TypeRowMapper = RowMapper<TypeDto> { rs, _ ->
    TypeDto(
        typeId = rs.getInt("typeId"),
        typeCode = rs.getString("typeCode"),
        typeDescription = rs.getString("typeDescription"),
        status = rs.getString("status")
    )
}

val UserNotificationsRowMapper = RowMapper<UserNotificationsDto> { rs, _ ->
    UserNotificationsDto(
        userNotificationId = rs.getInt("userNotificationId"),
        notificationType = rs.getString("notificationType"),
        userId = rs.getInt("userId"),
        message = rs.getString("message"),
        isExpiredFlag = rs.getBoolean("isExpiredFlag"),
        publishedFlag = rs.getBoolean("publishedFlag"),
        readFlag = rs.getBoolean("readFlag"),
        status = rs.getString("status"),
        createdAt = rs.getString("createdAt"),
        readAt = rs.getString("readAt"),
        expiredAt = rs.getString("expiredAt")

    )
}

/* ============================================================
 * FEEDBACK & SECURITY HISTORY
 * ============================================================ */
val FeedbackRowMapper = RowMapper<FeedbackDto> { rs, _ ->
    FeedbackDto(
        feedbackId = rs.getInt("feedbackId"),
        userId = rs.getInt("userId"),
        message = rs.getString("message"),
        timestamp = rs.getString("timestamp")
    )
}

val UserPasswordHistoryRowMapper = RowMapper<UserPasswordHistoryDto> { rs, _ ->
    UserPasswordHistoryDto(
        id = rs.getInt("id"),
        userId = rs.getInt("userId"),
        passwordHash = rs.getString("passwordHash"),
        changedAt = rs.getLong("changedAt")
    )
}

val UserPinHistoryRowMapper = RowMapper<UserPinHistoryDto> { rs, _ ->
    UserPinHistoryDto(
        id = rs.getInt("id"),
        userId = rs.getInt("userId"),
        pinHash = rs.getString("pinHash"),
        changedAt = rs.getLong("changedAt")
    )
}

/* ============================================================
 * USER + GROUP PROJECTION
 * (matches your toUserWithGroupDto() fields)
 * ============================================================ */
val UserWithGroupRowMapper = RowMapper<UserWithGroupDto> { rs, _ ->
    UserWithGroupDto(
        userId = rs.getInt("userId"),
        firstName = rs.getString("firstName"),
        lastName = rs.getString("lastName"),
        emailId = rs.getString("emailId"),
        phoneNumber = rs.getString("phoneNumber"),
        status = rs.getString("status"),
        userCode = rs.getString("userCode"),
        isPinSet = rs.getBoolean("isPinSet"),
        groupId = rs.getNullableInt("groupId"),
        groupName = rs.getString("groupName"),
        moderator = rs.getNullableInt("moderator"),
        description = rs.getString("description"),
        groupStatus = rs.getString("groupStatus")
    )
}

/* ============================================================
 * Registration helper
 * ============================================================ */
fun Jdbi.registerAppMappers(): Jdbi = this.apply {
    registerRowMapper(UsersDto::class.java, UsersRowMapper)
    registerRowMapper(GroupsDto::class.java, GroupsRowMapper)
    registerRowMapper(GroupMembersDto::class.java, GroupMembersRowMapper)
    registerRowMapper(GroupMemberWithNameDto::class.java, GroupMemberWithNameRowMapper)

    registerRowMapper(RolesDto::class.java, RolesRowMapper)
    registerRowMapper(PrivilegeDto::class.java, PrivilegeRowMapper)
    registerRowMapper(UserRolesDto::class.java, UserRolesRowMapper)
    registerRowMapper(RolePrivilegeDto::class.java, RolePrivilegeRowMapper)

    registerRowMapper(FundsDto::class.java, FundsRowMapper)
    registerRowMapper(FundMembersDto::class.java, FundMembersRowMapper)
    registerRowMapper(FundDetailsDto::class.java, FundDetailsRowMapper)

    registerRowMapper(DepositsDto::class.java, DepositsRowMapper)

    registerRowMapper(LoansDto::class.java, LoansRowMapper)
    registerRowMapper(LoanDetailsDto::class.java, LoanDetailsRowMapper)
    registerRowMapper(LoanEmisDto::class.java, LoanEmisRowMapper)

    registerRowMapper(TypeDto::class.java, TypeRowMapper)
    registerRowMapper(UserNotificationsDto::class.java, UserNotificationsRowMapper)

    registerRowMapper(FeedbackDto::class.java, FeedbackRowMapper)
    registerRowMapper(UserPasswordHistoryDto::class.java, UserPasswordHistoryRowMapper)
    registerRowMapper(UserPinHistoryDto::class.java, UserPinHistoryRowMapper)

    registerRowMapper(UserWithGroupDto::class.java, UserWithGroupRowMapper)
}
