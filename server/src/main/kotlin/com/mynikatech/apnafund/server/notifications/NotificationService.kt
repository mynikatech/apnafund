package com.mynikatech.apnafund.server.notifications

import com.mynikatech.apnafund.net.dto.AdminUserDto
import com.mynikatech.apnafund.net.dto.Channel
import com.mynikatech.apnafund.net.dto.EmailPayload
import com.mynikatech.apnafund.net.dto.FundModeratorDto
import com.mynikatech.apnafund.net.dto.NotificationEvent
import com.mynikatech.apnafund.net.dto.UserBasicDto
import com.mynikatech.apnafund.net.dto.UserNotificationsDto
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.server.common.messaging.dispatch.EventDispatchService
import com.mynikatech.apnafund.server.common.messaging.factories.GroupNotificationFactory
import com.mynikatech.apnafund.server.funds.FundsSql
import com.mynikatech.apnafund.server.groups.GroupsSql
import com.mynikatech.apnafund.server.loans.LoansSql
import com.mynikatech.apnafund.server.users.UsersSql
import org.slf4j.LoggerFactory

class NotificationService(
    private val sql: NotificationsSql,
    private val groupsSql: GroupsSql,
    private val fundsSql: FundsSql,
    private val usersSql: UsersSql,
    private val loansSql: LoansSql,
    private val eventDispatchService: EventDispatchService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun notifyFundCreated(
        fundId: Int,
        fundName: String,
        groupId: Int
    ) {
        try {

            logger.info("The group id is : $groupId")

            val groupMembers =
                groupsSql.getAllMembersofGroupWithNames(groupId, true)

            logger.info("GroupId=$groupId, groupMembers count=${groupMembers.size}")

            val fundMembers =
                fundsSql.getFundMembersWithNamesForFund(fundId)

            if (groupMembers.isEmpty()) {
                logger.info("No group members found")
                return
            }

            logger.info("Inserting into user notifications")

            groupMembers.forEach { member ->

                logger.info("Inserting notification for userId=${member.userId}")

                sql.addUserNotification(
                    UserNotificationsDto(
                        notificationType = "FUND_CREATED",
                        userId = member.userId,
                        message = "New fund \"$fundName\" has been created in your group.",
                        publishedFlag = true,
                        readFlag = false,
                        isExpiredFlag = false,
                        status = "ACTIVE"

                    )
                )

                logger.info("Inserted notification for userId=${member.userId}")
            }

            logger.info("Inserted into user notifications")

            val fundMemberNames =
                if (fundMembers.isEmpty()) {
                    "No members added yet"
                } else
                    fundMembers.joinToString(", ") { it.fullName }
            logger.info("The fund Member Names are $fundMemberNames")

            groupMembers.forEach { member ->
                logger.info("Dispatching email to ${member.emailId}")

                val event = GroupNotificationFactory.fundCreated(
                    fundId = fundId.toString(),
                    fundName = fundName,
                    recipient = member,
                    fundMemberNames = fundMemberNames
                )

                eventDispatchService.dispatchUser(event)

                logger.info("Email dispatched to ${member.emailId}")
            }

            logger.info("notifyFundCreated completed successfully")

        } catch (e: Exception) {
            logger.error("ERROR inside notifyFundCreated", e)
        }
    }


    fun notifyFundMembersAdded(
        fundId: Int,
        fundName: String,
        newMembers: List<UserBasicDto>
    ) {

        if (newMembers.isEmpty()) return

        // Insert in-app notifications
        newMembers.forEach { member ->
            sql.addUserNotification(
                UserNotificationsDto(
                    notificationType = "FUND_MEMBER_ADDED",
                    userId = member.userId,
                    message = "You have been added to fund '$fundName'.",
                    publishedFlag = true,
                    readFlag = false,
                    isExpiredFlag = false,
                    status = "ACTIVE"
                )
            )
        }

        // Dispatch email per newly added member

        newMembers.forEach { member ->

            val event = NotificationEvent(
                eventType = "FUND_MEMBER_ADDED",
                userId = member.userId.toString(),
                channels = setOf(Channel.EMAIL),

                email = EmailPayload(
                    to = member.emailId,
                    userName = member.fullName,
                    data = mapOf(
                        "fundName" to fundName
                    )
                ),

                eventData = mapOf(
                    "fundId" to fundId.toString(),
                    "fundName" to fundName
                )
            )
            eventDispatchService.dispatchUser(event)
        }

    }

    fun notifyFundClosed(
        fundId: Int,
        fundName: String,
        closedByName: String,
        reason: String
    ) {

        try {

            val fundMembers =
                fundsSql.getFundMembersWithNamesForFund(fundId)

            if (fundMembers.isEmpty()) {
                logger.info("No fund members found for closure notification")
                return
            }

            logger.info("Sending FUND_CLOSED notification to ${fundMembers.size} members")

            // ---------- IN APP NOTIFICATIONS ----------
            fundMembers.forEach { member ->

                sql.addUserNotification(
                    UserNotificationsDto(
                        notificationType = "FUND_CLOSED",
                        userId = member.userId,
                        message =
                            "Fund \"$fundName\" has been closed by $closedByName.",
                        publishedFlag = true,
                        readFlag = false,
                        isExpiredFlag = false,
                        status = "ACTIVE"
                    )
                )
            }

            // ---------- EMAIL DISPATCH ----------
            fundMembers.forEach { member ->

                val event = NotificationEvent(
                    eventType = "FUND_CLOSED",
                    userId = member.userId.toString(),
                    channels = setOf(Channel.EMAIL),

                    email = EmailPayload(
                        to = member.emailId,
                        userName = member.fullName,
                        data = mapOf(
                            "fundName" to fundName,
                            "closedBy" to closedByName,
                            "reason" to reason
                        )
                    ),

                    eventData = mapOf(
                        "fundId" to fundId.toString(),
                        "fundName" to fundName
                    )
                )

                eventDispatchService.dispatchUser(event)
            }

            logger.info("notifyFundClosed completed successfully")

        } catch (e: Exception) {
            logger.error("ERROR inside notifyFundClosed", e)
        }
    }


    fun notifyLoanRequested(
        loanId: Int,
        fundId: Int,
        fundName: String,
        borrowerName: String,
        moderators: List<FundModeratorDto>
    ) {

        try {

            val loan = loansSql.getLoanById(loanId) ?: run {

                logger.error(
                    "Loan not found for loanId=$loanId"
                )

                return
            }

            moderators.forEach { moderator ->

                try {

                    logger.info(
                        "Sending LOAN_REQUESTED to moderator ${moderator.userId}"
                    )

                    // ---------- IN APP ----------
                    sql.addUserNotification(
                        UserNotificationsDto(
                            notificationType = "LOAN_REQUESTED",

                            userId = moderator.userId,

                            message =
                                "$borrowerName has requested a loan of ₹${loan.loanAmount} in fund \"$fundName\".",

                            publishedFlag = true,
                            readFlag = false,
                            isExpiredFlag = false,
                            status = "ACTIVE"
                        )
                    )

                    // ---------- EMAIL ----------
                    val event = NotificationEvent(

                        eventType = "LOAN_REQUESTED",

                        userId = moderator.userId.toString(),

                        channels = setOf(Channel.EMAIL),

                        email = EmailPayload(
                            to = moderator.emailId!!,
                            userName = moderator.fullName(),

                            data = mapOf(
                                "borrowerName" to borrowerName,
                                "fundName" to fundName,
                                "loanId" to loanId.toString(),
                                "loanAmount" to loan.loanAmount.toString(),
                                "issueDate" to loan.issuedDate.toString(),
                                "loanPeriod" to loan.period.toString()
                            )
                        ),

                        eventData = mapOf(
                            "loanId" to loanId.toString(),
                            "fundId" to fundId.toString(),
                            "fundName" to fundName
                        )
                    )

                    eventDispatchService.dispatchUser(event)

                } catch (e: Exception) {

                    logger.error(
                        "Error notifying moderator ${moderator.userId}",
                        e
                    )
                }
            }

            logger.info(
                "notifyLoanRequested completed"
            )

        } catch (e: Exception) {

            logger.error(
                "ERROR inside notifyLoanRequested",
                e
            )
        }
    }

    fun notifyLoanApproved(
        loanId: Int,
        fundId: Int,
        fundName: String,
        borrower: UsersDto,
        approvedByName: String
    ) {

        try {

            val fundMembers =
                fundsSql.getFundMembersWithNamesForFund(fundId)

            if (fundMembers.isEmpty()) return

            val loan = loansSql.getLoanById(loanId)
            if (loan == null) {
                logger.error("Loan not found for loanId=$loanId. Cannot send LOAN_APPROVED notification.")
                return
            }

            logger.info("Sending LOAN_APPROVED to ${fundMembers.size} members")

            fundMembers.forEach { member ->

                val loanAmount = loan.loanAmount
                val issueDate = loan.issuedDate
                val maturityDate = loan.maturityDate

                val message =
                    if (member.userId == borrower.userId)
                        "Your loan (₹$loanAmount) in fund \"$fundName\" has been approved."
                    else
                        "${borrower.fullName} has been approved a loan of ₹$loanAmount in fund \"$fundName\"."

                // ---------- IN APP ----------
                sql.addUserNotification(
                    UserNotificationsDto(
                        notificationType = "LOAN_APPROVED",
                        userId = member.userId,
                        message = message,
                        publishedFlag = true,
                        readFlag = false,
                        isExpiredFlag = false,
                        status = "ACTIVE"
                    )
                )

                // ---------- EMAIL ----------
                val event = NotificationEvent(
                    eventType = "LOAN_APPROVED",
                    userId = member.userId.toString(),
                    channels = setOf(Channel.EMAIL),

                    email = EmailPayload(
                        to = member.emailId,
                        userName = member.fullName,
                        data = mapOf(
                            "fundName" to fundName,
                            "borrowerName" to borrower.fullName,
                            "approvedBy" to approvedByName,
                            "loanAmount" to loanAmount.toString(),
                            "issueDate" to issueDate.toString(),
                            "maturityDate" to maturityDate.toString(),
                            "loanId" to loanId.toString()
                        )
                    ),

                    eventData = mapOf(
                        "loanId" to loanId.toString(),
                        "fundId" to fundId.toString()
                    )
                )

                eventDispatchService.dispatchUser(event)
            }

            logger.info("notifyLoanApproved completed")

        } catch (e: Exception) {
            logger.error("ERROR inside notifyLoanApproved", e)
        }
    }

    fun notifyLoanRejected(
        loanId: Int,
        fundName: String,
        borrower: UsersDto,
        rejectedBy: String,
        reason: String
    ) {

        try {

            val loan = loansSql.getLoanById(loanId) ?: run {
                logger.error("Loan not found for loanId=$loanId")
                return
            }

            sql.addUserNotification(
                UserNotificationsDto(
                    notificationType = "LOAN_REJECTED",
                    userId = borrower.userId!!,
                    message =
                        "Your loan request of ₹${loan.loanAmount} in \"$fundName\" was rejected.",
                    publishedFlag = true,
                    readFlag = false,
                    isExpiredFlag = false,
                    status = "ACTIVE"
                )
            )

            val event = NotificationEvent(
                eventType = "LOAN_REJECTED",
                userId = borrower.userId.toString(),
                channels = setOf(Channel.EMAIL),

                email = EmailPayload(
                    to = borrower.emailId,
                    userName = borrower.fullName,
                    data = mapOf(
                        "fundName" to fundName,
                        "rejectedBy" to rejectedBy,
                        "reason" to reason,
                        "loanId" to loanId.toString(),
                        "loanAmount" to loan.loanAmount.toString(),
                        "issueDate" to loan.issuedDate.toString()
                    )
                ),

                eventData = mapOf(
                    "loanId" to loanId.toString()
                )
            )

            eventDispatchService.dispatchUser(event)

        } catch (e: Exception) {
            logger.error("ERROR inside notifyLoanRejected", e)
        }
    }

    fun notifyGroupRequested(
        groupId: Int,
        groupName: String,
        requestorName: String,
        approvers: List<AdminUserDto>
    ) {

        try {

            val group =
                groupsSql.getGroup(groupId)
                    .firstOrNull()
                    ?: run {

                        logger.error(
                            "Group not found for groupId=$groupId"
                        )

                        return
                    }

            approvers.forEach { approver ->

                try {

                    logger.info(
                        "Sending GROUP_REQUESTED to admin ${approver.userId}"
                    )

                    // ---------- IN APP ----------
                    sql.addUserNotification(
                        UserNotificationsDto(

                            notificationType =
                                "GROUP_REQUESTED",

                            userId =
                                approver.userId!!,

                            message =
                                "$requestorName has requested a new group: $groupName.",

                            publishedFlag = true,
                            readFlag = false,
                            isExpiredFlag = false,
                            status = "ACTIVE"
                        )
                    )

                    // ---------- EMAIL ----------
                    val event = NotificationEvent(

                        eventType = "GROUP_REQUESTED",

                        userId =
                            approver.userId.toString(),

                        channels =
                            setOf(Channel.EMAIL),

                        email = EmailPayload(
                            to = approver.emailId,

                            userName =
                                approver.fullName,

                            data = mapOf(
                                "requestorName" to requestorName,

                                "groupName" to groupName,

                                "groupDescription" to
                                        (
                                                group.description
                                                    ?: ""
                                                )
                            )
                        ),

                        eventData = mapOf(
                            "groupId" to groupId.toString(),
                            "groupName" to groupName
                        )
                    )

                    eventDispatchService.dispatchUser(
                        event
                    )

                } catch (e: Exception) {

                    logger.error(
                        "Error notifying admin ${approver.userId}",
                        e
                    )
                }
            }

            logger.info(
                "notifyGroupRequested completed"
            )

        } catch (e: Exception) {

            logger.error(
                "ERROR inside notifyGroupRequested",
                e
            )
        }
    }

    fun notifyGroupApproved(
        groupId: Int
    ) {

        try {

            val group =
                groupsSql.getGroup(groupId).firstOrNull()
                    ?: error("Group required")

            val moderatorId = group.moderator ?: error("Moderator required")
            val moderatorUser = usersSql.getUserById(moderatorId)

            logger.info("Sending GROUP_APPROVED notification")

            // ---------- IN APP ----------
            sql.addUserNotification(
                UserNotificationsDto(
                    notificationType = "GROUP_APPROVED",
                    userId = moderatorUser.userId!!,
                    message =
                        "Your group \"${group.groupName}\" has been approved.",
                    publishedFlag = true,
                    readFlag = false,
                    isExpiredFlag = false,
                    status = "ACTIVE"
                )
            )

            // ---------- EMAIL ----------
            val event = NotificationEvent(
                eventType = "GROUP_APPROVED",
                userId = moderatorUser.userId.toString(),
                channels = setOf(Channel.EMAIL),

                email = EmailPayload(
                    to = moderatorUser.emailId,
                    userName = moderatorUser.fullName,
                    data = mapOf(
                        "groupName" to group.groupName
                    )
                ),

                eventData = mapOf(
                    "groupId" to groupId.toString(),
                    "groupName" to group.groupName
                )
            )

            eventDispatchService.dispatchUser(event)

            logger.info("notifyGroupApproved completed")

        } catch (e: Exception) {
            logger.error("ERROR inside notifyGroupApproved", e)
        }
    }

    fun notifyGroupRejected(
        groupId: Int,
        reason: String
    ) {

        try {

            val group =
                groupsSql.getGroup(groupId).firstOrNull()
                    ?: error("Group required")

            val moderatorId = group.moderator ?: error("Moderator required")
            val moderatorUser = usersSql.getUserById(moderatorId)

            logger.info("Sending GROUP_REJECTED notification")

            // ---------- IN APP ----------
            sql.addUserNotification(
                UserNotificationsDto(
                    notificationType = "GROUP_REJECTED",
                    userId = moderatorUser.userId!!,
                    message =
                        "Your group \"${group.groupName}\" was not approved.",
                    publishedFlag = true,
                    readFlag = false,
                    isExpiredFlag = false,
                    status = "ACTIVE"
                )
            )

            // ---------- EMAIL ----------
            val event = NotificationEvent(
                eventType = "GROUP_REJECTED",
                userId = moderatorUser.userId.toString(),
                channels = setOf(Channel.EMAIL),

                email = EmailPayload(
                    to = moderatorUser.emailId,
                    userName = moderatorUser.fullName,
                    data = mapOf(
                        "groupName" to group.groupName,
                        "reason" to reason
                    )
                ),

                eventData = mapOf(
                    "groupId" to groupId.toString(),
                    "groupName" to group.groupName
                )
            )

            eventDispatchService.dispatchUser(event)

            logger.info("notifyGroupRejected completed")

        } catch (e: Exception) {
            logger.error("ERROR inside notifyGroupRejected", e)
        }


    }

    fun notifyLoanClosed(
        loanId: Int,
        fundId: Int,
        fundName: String,
        borrower: UsersDto,
        closureType: String,
        closedByName: String
    ) {

        try {

            val fundMembers =
                fundsSql.getFundMembersWithNamesForFund(fundId)

            if (fundMembers.isEmpty()) {
                logger.info("No fund members found for loan closure notification")
                return
            }

            val loan = loansSql.getLoanById(loanId)
            if (loan == null) {
                logger.error("Loan not found for loanId=$loanId")
                return
            }

            logger.info("Sending LOAN_CLOSED to ${fundMembers.size} members")

            fundMembers.forEach { member ->

                val message =
                    if (member.userId == borrower.userId)
                        "Your loan (₹${loan.loanAmount}) in fund \"$fundName\" has been closed."
                    else
                        "${borrower.fullName}'s loan (₹${loan.loanAmount}) in fund \"$fundName\" has been closed."

                // ---------- IN APP ----------
                sql.addUserNotification(
                    UserNotificationsDto(
                        notificationType = "LOAN_CLOSED",
                        userId = member.userId,
                        message = message,
                        publishedFlag = true,
                        readFlag = false,
                        isExpiredFlag = false,
                        status = "ACTIVE"
                    )
                )

                // ---------- EMAIL ----------
                val event = NotificationEvent(
                    eventType = "LOAN_CLOSED",
                    userId = member.userId.toString(),
                    channels = setOf(Channel.EMAIL),

                    email = EmailPayload(
                        to = member.emailId,
                        userName = member.fullName,
                        data = mapOf(
                            "fundName" to fundName,
                            "borrowerName" to borrower.fullName,
                            "loanAmount" to loan.loanAmount.toString(),
                            "closureType" to closureType,
                            "closedBy" to closedByName,
                            "loanId" to loanId.toString()
                        )
                    ),

                    eventData = mapOf(
                        "loanId" to loanId.toString(),
                        "fundId" to fundId.toString()
                    )
                )

                eventDispatchService.dispatchUser(event)
            }

            logger.info("notifyLoanClosed completed")

        } catch (e: Exception) {
            logger.error("ERROR inside notifyLoanClosed", e)
        }
    }

    fun notifyLoanClosureRequested(
        loanId: Int,
        fundId: Int,
        fundName: String,
        borrowerName: String,
        moderators: List<FundModeratorDto>,
        requestedAmount: Double?,
        remarks: String?
    ) {

        try {

            val loan = loansSql.getLoanById(loanId) ?: run {
                logger.error("Loan not found for loanId=$loanId")
                return
            }

            logger.info("Sending LOAN_CLOSURE_REQUESTED to moderators")
            moderators.forEach { moderator ->

                try {
                    // ---------- IN APP ----------
                    sql.addUserNotification(
                        UserNotificationsDto(
                            notificationType = "LOAN_CLOSURE_REQUESTED",
                            userId = moderator.userId!!,
                            message =
                                "$borrowerName has requested closure of loan (₹${loan.loanAmount}) in fund \"$fundName\".",
                            publishedFlag = true,
                            readFlag = false,
                            isExpiredFlag = false,
                            status = "ACTIVE"
                        )
                    )
                    if (!moderator.emailId.isNullOrBlank()) {
                        // ---------- EMAIL ----------
                        val event = NotificationEvent(
                            eventType = "LOAN_CLOSURE_REQUESTED",
                            userId = moderator.userId.toString(),
                            channels = setOf(Channel.EMAIL),

                            email = EmailPayload(
                                to = moderator.emailId ?: "",
                                userName = moderator.fullName!!,
                                data = mapOf(
                                    "borrowerName" to borrowerName,
                                    "fundName" to fundName,
                                    "loanId" to loanId.toString(),
                                    "loanAmount" to loan.loanAmount.toString(),
                                    "loanNumber" to loan.loanNumber,
                                    "issuedDate" to loan.issuedDate,
                                    "maturityDate" to loan.maturityDate,
                                    "requestedAmount" to (requestedAmount?.toString() ?: ""),
                                    "remarks" to (remarks ?: "")
                                )
                            ),

                            eventData = mapOf(
                                "loanId" to loanId.toString(),
                                "fundId" to fundId.toString(),
                                "fundName" to fundName
                            )
                        )

                        eventDispatchService.dispatchUser(event)
                    }
                } catch (e: Exception) {

                    logger.error(
                        "Error notifying moderator ${moderator.userId}",
                        e
                    )
                }
            }

            logger.info("notifyLoanClosureRequested completed")

        } catch (e: Exception) {
            logger.error("ERROR inside notifyLoanClosureRequested", e)
        }
    }

    fun notifyLoanClosureRejected(
        loanId: Int,
        fundName: String,
        borrower: UsersDto,
        rejectedByName: String,
        reason: String
    ) {

        try {

            val loan = loansSql.getLoanById(loanId)
            if (loan == null) {
                logger.error("Loan not found for loanId=$loanId")
                return
            }

            logger.info("Sending LOAN_CLOSURE_REJECTED to borrower ${borrower.userId}")

            val message =
                "Your loan closure request for ₹${loan.loanAmount} in fund \"$fundName\" was rejected."

            // ---------- IN APP ----------
            sql.addUserNotification(
                UserNotificationsDto(
                    notificationType = "LOAN_CLOSURE_REJECTED",
                    userId = borrower.userId!!,
                    message = message,
                    publishedFlag = true,
                    readFlag = false,
                    isExpiredFlag = false,
                    status = "ACTIVE"
                )
            )

            // ---------- EMAIL ----------
            val event = NotificationEvent(
                eventType = "LOAN_CLOSURE_REJECTED",
                userId = borrower.userId.toString(),
                channels = setOf(Channel.EMAIL),

                email = EmailPayload(
                    to = borrower.emailId,
                    userName = borrower.fullName,
                    data = mapOf(
                        "fundName" to fundName,
                        "loanId" to loanId.toString(),
                        "loanAmount" to loan.loanAmount.toString(),
                        "rejectedBy" to rejectedByName,
                        "reason" to reason
                    )
                ),

                eventData = mapOf(
                    "loanId" to loanId.toString()
                )
            )

            eventDispatchService.dispatchUser(event)

            logger.info("notifyLoanClosureRejected completed")

        } catch (e: Exception) {
            logger.error("ERROR inside notifyLoanClosureRejected", e)
        }
    }
}
