package com.mynikatech.apnafund.server.users

import com.mynikatech.apnafund.net.dto.ModeratorRegistrationResponse
import com.mynikatech.apnafund.net.dto.RegisterModeratorRequest
import com.mynikatech.apnafund.server.approval.ApprovalSql
import com.mynikatech.apnafund.server.auth.FirebaseGroupService
import com.mynikatech.apnafund.server.common.messaging.dispatch.EventDispatchService
import com.mynikatech.apnafund.server.common.messaging.factories.UserNotificationFactory
import com.mynikatech.apnafund.server.groups.GroupsSql
import com.mynikatech.apnafund.server.roles.RolesSql
import com.mynikatech.apnafund.server.security.PasswordHistorySql
import com.mynikatech.apnafund.server.userroles.UserRolesSql
import org.slf4j.LoggerFactory

class ModeratorRegistrationService(
    private val usersSql: UsersSql,
    private val userRolesSql: UserRolesSql,
    private val rolesSql: RolesSql,
    private val groupsSql: GroupsSql,
    private val emailVerificationService: EmailVerificationService,
    private val eventDispatchService: EventDispatchService,
    private val emailVerificationEnabled: Boolean,
    private val passwordHistSql: PasswordHistorySql,
    private val approvalSql: ApprovalSql
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun registerModeratorAndGroup(
        req: RegisterModeratorRequest
    ): ModeratorRegistrationResponse {

        val user = req.user

        /* -------------------------------------------------
         * 1️⃣ CREATE / UPSERT USER
         * ------------------------------------------------- */
        val userId = usersSql.upsertUserByEmail(user)

        /* ---- Add Passwordinto History Table ***/
        val newHash = req.user.passwordHash
        if (!newHash.isNullOrBlank()) {
            val lastHashes = passwordHistSql.getLast3PasswordHashes(userId)
            if (lastHashes.isEmpty() || lastHashes.first() != newHash) {
                passwordHistSql.insertPasswordHistory(
                    userId = userId,
                    passwordHash = newHash
                )
            }
        }


        /* -------------------------------------------------
         * 2️⃣ ASSIGN MEMBER ROLE (ACTIVE)
         * ------------------------------------------------- */
        val memberRoleId =
            rolesSql.getRoleIdByRoleCode("MEMBER")
                ?: error("MEMBER role not found")

        userRolesSql.addUserRole(
            userId = userId,
            roleId = memberRoleId,
            status = "ACTIVE"
        )

        /* -------------------------------------------------
         * 3️⃣ EMAIL VERIFICATION (ENV AWARE)
         * ------------------------------------------------- */
        val emailVerified: Boolean
        val expiresAtMillis: Long?

        if (emailVerificationEnabled) {
            expiresAtMillis = emailVerificationService.sendVerificationEmail(
                userId = userId,
                email = user.emailId,
                userName = "${user.firstName} ${user.lastName}",
                purpose = "EMAIL_VERIFY"
            )
            emailVerified = false
        } else {
            // DEV / TEST auto-verify
            usersSql.markUserEmailVerified(userId)
            expiresAtMillis = null
            emailVerified = true
        }

        /* -------------------------------------------------
         * 4️⃣ ASSIGN MODERATOR ROLE (PENDING)
         * ------------------------------------------------- */
        val moderatorRoleId =
            rolesSql.getRoleIdByRoleCode("MODERATOR")
                ?: error("MODERATOR role not found")

        userRolesSql.addUserRole(
            userId = userId,
            roleId = moderatorRoleId,
            status = "PENDING"
        )

        /* -------------------------------------------------
         * 5️⃣ CREATE GROUP (PENDING, MODERATOR = USER)
         * ------------------------------------------------- */
        val group = groupsSql.addGroup(
            req.group.copy(
                moderator = userId,      // ✅ SET HERE
                status = "PENDING"
            ))

        val groupId = group.groupId ?: error("Group id missing")

        // Create the pending approval requests
        val adminUser =
            usersSql.getAdminUser()
                ?: throw IllegalStateException("Admin user not found")
        approvalSql.createGroupApproval(
            groupId,
            userId,
            adminUser.userId!!
        )

        FirebaseGroupService.createGroup(
            groupId = groupId,
            groupName = req.group.groupName
        )

        /* -------------------------------------------------
         * 6️⃣ NOTIFY ADMIN
         * ------------------------------------------------- */
        try {
            eventDispatchService.dispatchUser(
                UserNotificationFactory.adminGroupPendingApproval(
                    moderatorName = "${user.firstName} ${user.lastName}",
                    moderatorEmail = user.emailId,
                    groupName = req.group.groupName,
                    groupId = groupId
                )
            )
        } catch (ex: Exception) {
            logger.error("Failed to send admin approval email", ex)
        }

        return ModeratorRegistrationResponse(
            userId = userId,
            emailVerified = emailVerified,
            emailOtpExpiresAtMillis = expiresAtMillis
        )
    }
}
