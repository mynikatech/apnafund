package com.mynikatech.apnafund.lambda.email

import AdminGroupPendingApprovalEmail
import ModeratorGroupApprovedEmail
import com.mynikatech.apnafund.net.dto.NotificationEvent

object UserNotificationContentRegistry {

    data class Content(
        val subject: String,
        val message: (NotificationEvent) -> String
    )

    private val registry: Map<String, Content> = mapOf(

        /* ---------------- USER EVENTS ---------------- */

        "USER_REGISTERED" to Content(
            subject = "Welcome to ApnaFund",
            message = {
                """
                <p>Your account has been created successfully.</p>
                """.trimIndent()
            }
        ),

        "USER_UPDATED" to Content(
            subject = "Account Updated",
            message = {
                """
                <p>Your account details were updated successfully.</p>
                """.trimIndent()
            }
        ),

        /* ---------------- FUND EVENTS ---------------- */

        "FUND_CLOSED" to Content(

            subject = "Fund Closed",

            message = { event ->

                val fundName =
                    event.eventData["fundName"] ?: "your fund"
                val closedBy =
                    event.email?.data?.get("closedBy") ?: "Moderator"

                val reason =
                    event.email?.data?.get("reason") ?: "No reason provided"

                val userName =
                    event.email?.userName ?: "User"

                FundClosureEmail.body(
                    userName = userName,
                    fundName = fundName,
                    closedByName = closedBy,
                    reason = reason
                )
            }
        ),

        /* ---------------- ADMIN EVENTS ---------------- */

        "ADMIN_GROUP_PENDING_APPROVAL" to Content(
            subject = AdminGroupPendingApprovalEmail.subject(),
            message = { event ->
                AdminGroupPendingApprovalEmail.body(
                    moderatorName = event.eventData["moderatorName"] ?: "",
                    groupName = event.eventData["groupName"] ?: ""
                )
            }
        ),

        "MODERATOR_GROUP_APPROVED" to Content(
            subject = ModeratorGroupApprovedEmail.subject(),
            message = { event ->
                ModeratorGroupApprovedEmail.body(
                    moderatorName = event.eventData["moderatorName"] ?: "",
                    groupName = event.eventData["groupName"] ?: ""
                )
            }
        ),
        "MODERATOR_GROUP_REJECTED" to Content(
            subject = ModeratorGroupRejectedEmail.subject(),
            message = { event ->
                ModeratorGroupRejectedEmail.body(
                    moderatorName = event.eventData["moderatorName"] ?: "",
                    groupName = event.eventData["groupName"] ?: "",
                    reason = event.eventData["reason"]
                )
            }
        ),
        "EMAIL_VERIFICATION" to Content(
            subject = EmailVerificationEmail.subject(),
            message = { event ->
                val email = event.email
                    ?: error("Email payload missing for EMAIL_VERIFICATION")

                EmailVerificationEmail.body(
                    userName = email.userName,
                    otp = email.data["otp"]
                        ?: error("OTP missing for EMAIL_VERIFICATION")
                )
            }
        ),
        "EMAIL_VERIFIED_WELCOME" to Content(
            subject = "Welcome to ApnaFund 🎉",
            message = { event ->
                val email = event.email
                    ?: error("Email payload missing for EMAIL_VERIFIED_WELCOME")

                EmailVerifiedWelcomeEmail.body(
                    userName = email.userName
                )
            }
        ),
        "INVITE_NOTICE" to Content(
            subject = "You’re invited to join Apna Fund 🎉",
            message = { event ->
                val email = event.email
                    ?: error("Email payload missing for INVITE_NOTICE")

                InviteEmail.body(
                    userName = email.userName,
                    invitedBy = email.data.getValue("invitedBy"),
                    groupName = email.data.getValue("groupName")
                )
            }
        ),
        "FUND_CREATED" to Content(
            subject = "New Fund Created",
            message = { event ->

                val fundName = event.eventData["fundName"] ?: "your fund"
                val fundMembers = event.eventData["fundMembers"] ?: "No members added yet"

                val email = event.email
                    ?: error("Email payload missing for FUND_CREATED")

                FundCreatedEmail.body(
                    userName = email.userName,
                    fundName = fundName,
                    fundMembers = fundMembers
                )
            }
        ),
        "GROUP_APPROVED" to Content(
            subject = "Group Approved",
            message = { event ->
                GroupApprovedEmail.body(
                    event.email?.userName ?: "User",
                    event.eventData["groupName"] ?: ""
                )
            }
        ),

        "GROUP_REJECTED" to Content(
            subject = "Group Rejected",
            message = { event ->
                GroupRejectedEmail.body(
                    event.email?.userName ?: "User",
                    event.eventData["groupName"] ?: ""
                )
            }
        ),
        "LOAN_REQUESTED" to Content(
            subject = LoanRequestEmail.subject(),
            message = { event ->
                LoanRequestEmail.body(
                    userName = event.email?.userName ?: "User",
                    borrowerName = event.email?.data?.get("borrowerName") ?: "",
                    fundName = event.email?.data?.get("fundName") ?: "",
                    loanId = event.email?.data?.get("loanId") ?: "",
                    loanAmount = event.email?.data?.get("loanAmount") ?: "",
                    issueDate = event.email?.data?.get("issueDate") ?: "",
                    loanPeriod = event.email?.data?.get("loanPeriod") ?: ""
                )
            }
        ),
        "LOAN_APPROVED" to Content(
            subject = LoanApprovedEmail.subject(),
            message = { event ->
                LoanApprovedEmail.body(
                    userName = event.email?.userName ?: "User",
                    fundName = event.email?.data?.get("fundName") ?: "",
                    approvedBy = event.email?.data?.get("approvedBy") ?: "Moderator",
                    loanId = event.email?.data?.get("loanId") ?: "",
                    loanAmount = event.email?.data?.get("loanAmount") ?: "",
                    issueDate = event.email?.data?.get("issueDate") ?: "",
                    maturityDate = event.email?.data?.get("maturityDate") ?: ""
                )
            }
        ),
        "LOAN_REJECTED" to Content(
            subject = LoanRejectedEmail.subject(),
            message = { event ->
                LoanRejectedEmail.body(
                    userName = event.email?.userName ?: "User",
                    fundName = event.email?.data?.get("fundName") ?: "",
                    rejectedBy = event.email?.data?.get("rejectedBy") ?: "Moderator",
                    reason = event.email?.data?.get("reason") ?: "No reason provided",
                    loanId = event.email?.data?.get("loanId") ?: "",
                    loanAmount = event.email?.data?.get("loanAmount") ?: "",
                    issueDate = event.email?.data?.get("issueDate") ?: ""
                )
            }
        ),
        "FUND_MEMBER_ADDED" to Content(
            subject = FundMemberAddedEmail.subject(),
            message = { event ->
                val email = event.email
                    ?: error("Email payload missing for FUND_MEMBER_ADDED")

                val fundName = event.eventData["fundName"]
                    ?: error("fundName missing for FUND_MEMBER_ADDED")

                FundMemberAddedEmail.body(
                    userName = email.userName,
                    fundName = fundName
                )
            }
        ),
        "GROUP_REQUESTED" to Content(
            subject = AdminGroupPendingApprovalEmail.subject(),
            message = { event ->
                AdminGroupPendingApprovalEmail.body(
                    moderatorName = event.eventData["moderatorName"] ?: "",
                    groupName = event.eventData["groupName"] ?: ""
                )
            }
        ),
    )

    fun get(event: NotificationEvent): Content {
        println("Looking up content for eventType=${event.eventType}")
        println("Available keys=${registry.keys}")

        return registry[event.eventType]
            ?: Content(
                subject = "ApnaFund Notification",
                message = {
                    "<p>You have a new notification from ApnaFund.</p>"
                }
            )
    }
}
