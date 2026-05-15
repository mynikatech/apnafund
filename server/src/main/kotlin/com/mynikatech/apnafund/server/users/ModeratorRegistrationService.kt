package com.mynikatech.apnafund.server.users

import com.mynikatech.apnafund.net.dto.ModeratorRegistrationResponse
import com.mynikatech.apnafund.net.dto.RegisterModeratorRequest
import com.mynikatech.apnafund.server.approval.ApprovalSql
import com.mynikatech.apnafund.server.auth.FirebaseGroupService
import com.mynikatech.apnafund.server.common.messaging.dispatch.EventDispatchService
import com.mynikatech.apnafund.server.common.messaging.factories.UserNotificationFactory
import com.mynikatech.apnafund.server.db.Db.jdbi
import com.mynikatech.apnafund.server.groups.GroupsSql
import com.mynikatech.apnafund.server.notifications.NotificationService
import com.mynikatech.apnafund.server.roles.RolesSql
import com.mynikatech.apnafund.server.security.PasswordHistorySql
import com.mynikatech.apnafund.server.userroles.UserRolesSql
import org.slf4j.LoggerFactory
import java.time.LocalDate

class ModeratorRegistrationService(
    private val usersSql: UsersSql,
    private val userRolesSql: UserRolesSql,
    private val rolesSql: RolesSql,
    private val groupsSql: GroupsSql,
    private val emailVerificationService: EmailVerificationService,
    private val notificationService: NotificationService,
    private val emailVerificationEnabled: Boolean,
    private val passwordHistSql: PasswordHistorySql,
    private val approvalSql: ApprovalSql
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun registerModeratorAndGroup(
        req: RegisterModeratorRequest
    ): ModeratorRegistrationResponse {

        val user = req.user
        val requestorId = req.requestorId

        val safeRequestorId =
            if (requestorId >= 0) requestorId else 1

        val txnResult  =
            jdbi.inTransaction<RegistrationTxnResult, Exception> { handle ->

                val usersSql =
                    handle.attach(UsersSql::class.java)

                val userRolesSql =
                    handle.attach(UserRolesSql::class.java)

                val rolesSql =
                    handle.attach(RolesSql::class.java)

                val groupsSql =
                    handle.attach(GroupsSql::class.java)

                val approvalSql =
                    handle.attach(ApprovalSql::class.java)

                val passwordHistSql =
                    handle.attach(PasswordHistorySql::class.java)

                /* -----------------------------------------
                 * CREATE / UPSERT USER
                 * ----------------------------------------- */

                val userId =
                    usersSql.upsertUserByEmail(
                        user,
                        safeRequestorId
                    )

                usersSql.updateAuditFields(
                    userId = userId,
                    createdByUserId = userId,
                    updatedByUserId = userId
                )

                /* -----------------------------------------
                 * PASSWORD HISTORY
                 * ----------------------------------------- */

                val newHash = req.user.passwordHash

                if (!newHash.isNullOrBlank()) {

                    val lastHashes =
                        passwordHistSql.getLast3PasswordHashes(userId)

                    if (
                        lastHashes.isEmpty() ||
                        lastHashes.first() != newHash
                    ) {

                        passwordHistSql.insertPasswordHistory(
                            userId = userId,
                            passwordHash = newHash
                        )
                    }
                }

                /* -----------------------------------------
                 * MEMBER ROLE
                 * ----------------------------------------- */

                val memberRoleId =
                    rolesSql.getRoleIdByRoleCode("MEMBER")
                        ?: error("MEMBER role not found")

                userRolesSql.addUserRole(
                    userId = userId,
                    roleId = memberRoleId,
                    status = "ACTIVE"
                )

                /* -----------------------------------------
                 * EMAIL VERIFICATION
                 * ----------------------------------------- */

                val emailVerified: Boolean
                val expiresAtMillis: Long?

                if (emailVerificationEnabled) {

                    expiresAtMillis =
                        emailVerificationService.sendVerificationEmail(
                            userId = userId,
                            email = user.emailId,
                            userName = "${user.firstName} ${user.lastName}",
                            purpose = "EMAIL_VERIFY"
                        )

                    emailVerified = false

                } else {

                    usersSql.markUserEmailVerified(userId)

                    expiresAtMillis = null

                    emailVerified = true
                }

                /* -----------------------------------------
                 * MODERATOR ROLE
                 * ----------------------------------------- */

                val moderatorRoleId =
                    rolesSql.getRoleIdByRoleCode("GROUP_MODERATOR")
                        ?: error("MODERATOR role not found")

                userRolesSql.addUserRole(
                    userId = userId,
                    roleId = moderatorRoleId,
                    status = "PENDING"
                )

                /* -----------------------------------------
                 * GROUP
                 * ----------------------------------------- */

                val group =
                    groupsSql.addGroup(
                        req.group.copy(
                            moderator = userId,
                            status = "PENDING"
                        )
                    )

                val groupId =
                    group.groupId
                        ?: error("Group id missing")

                groupsSql.addGroupMember(
                    userId,
                    groupId,
                    LocalDate.now().toString(),
                    "PRIMARY_MODERATOR",
                    userId,
                    "INACTIVE"
                )

                approvalSql.createGroupApproval(
                    groupId = groupId,
                    requestedBy = userId,
                    approver = null
                )

                RegistrationTxnResult(
                    response = ModeratorRegistrationResponse(
                        userId = userId,
                        emailVerified = emailVerified,
                        emailOtpExpiresAtMillis = expiresAtMillis
                    ),
                    groupId = groupId
                )
            }

        /* -------------------------------------------------
         * OUTSIDE TRANSACTION
         * ------------------------------------------------- */

//        try {
//
//            FirebaseGroupService.createGroup(
//                groupId = txnResult.groupId,
//                groupName = req.group.groupName
//            )
//
//        } catch (ex: Exception) {
//
//            logger.error(
//                "Firebase group creation failed",
//                ex
//            )
//        }

        try {

            val admins =
                usersSql.getAdminUsers()

            notificationService.notifyGroupRequested(
                groupId = txnResult.groupId,
                groupName = req.group.groupName,
                requestorName = "${user.firstName} ${user.lastName}",
                approvers = admins
            )

        } catch (ex: Exception) {

            logger.error(
                "Failed to send admin approval email",
                ex
            )
        }

        return txnResult.response
    }
}
