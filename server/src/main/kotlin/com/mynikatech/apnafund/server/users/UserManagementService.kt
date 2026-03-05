package com.mynikatech.apnafund.server.users

import com.mynikatech.apnafund.net.api.ValidationException
import com.mynikatech.apnafund.net.dto.RegisterOrUpdateUserRequest
import com.mynikatech.apnafund.net.dto.SaveOrUpdateUserResponse
import com.mynikatech.apnafund.net.dto.UserSaveSource
import com.mynikatech.apnafund.server.common.messaging.dispatch.EventDispatchService
import com.mynikatech.apnafund.server.common.messaging.factories.UserNotificationFactory
import com.mynikatech.apnafund.server.groups.GroupsSql
import com.mynikatech.apnafund.server.roles.RolesSql
import com.mynikatech.apnafund.server.security.PasswordHistorySql
import com.mynikatech.apnafund.server.userroles.UserRolesSql
import com.mynikatech.apnafund.server.util.Converters
import org.slf4j.LoggerFactory
import java.time.LocalDate

class UserManagementService(
    private val usersSql: UsersSql,
    private val userRolesSql: UserRolesSql,
    private val rolesSql: RolesSql,
    private val groupsSql: GroupsSql,
    private val eventDispatchService: EventDispatchService,
    private val emailVerificationService: EmailVerificationService,
    private val emailVerificationEnabled: Boolean,
    private val passwordHistSql: PasswordHistorySql
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun saveOrUpdateUser(req: RegisterOrUpdateUserRequest): SaveOrUpdateUserResponse {

        val user = req.user

        val userId: Int = usersSql.upsertUserByEmail(user)

        var emailVerified = false
        var expiresOtpAtMillis: Long? = null

        when (req.source) {

            UserSaveSource.SELF_REGISTER,
            UserSaveSource.MODERATOR_REGISTER -> {
                // insert into password history if present and diff from existing
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
                val userRoleId = addUserRole(userId, req.roleCode)
                handleGroup(userId, req.groupId)
                if (emailVerificationEnabled) {
                    expiresOtpAtMillis = emailVerificationService.sendVerificationEmail(
                        userId = userId,
                        email = user.emailId,
                        userName = "${user.firstName} ${user.lastName}",
                        purpose = "EMAIL_VERIFY"
                    )
                    emailVerified = false
                } else {
                    usersSql.markUserEmailVerified(userId)
                    eventDispatchService.dispatchUser(
                        UserNotificationFactory.devRegistrationNotice(
                            userId = userId,
                            email = user.emailId,
                            userName = "${user.firstName} ${user.lastName}"
                        )
                    )
                    emailVerified = true
                }
            }

            UserSaveSource.SELF_UPDATE -> {
                eventDispatchService.dispatchUser(
                    UserNotificationFactory.userUpdated(
                        userId = userId.toString(),
                        email = user.emailId,
                        phone = user.phoneNumber ?: "",
                        userName = "${user.firstName} ${user.lastName}"
                    )
                )
            }

            UserSaveSource.ADMIN_CREATE -> {
                val userRoleId = addUserRole(userId, req.roleCode)
                handleGroup(userId, req.groupId)
                val group = groupsSql.getGroup(req.groupId).firstOrNull()
                if(null != group) {
                    val moderatorId = group.moderator
                    val moderatorUser = moderatorId?.let {
                        usersSql.getUser(it).firstOrNull()
                    }
                    val invitedByName = moderatorUser?.fullName ?: "Group Moderator"
                    // INVITE EMAIL
                    logger.info("Sending email joining invite")
                    eventDispatchService.dispatchUser(
                        UserNotificationFactory.inviteNotice(
                            userId = userId,
                            email = user.emailId,
                            userName = "${user.firstName} ${user.lastName}",
                            invitedBy = invitedByName, // moderator name
                            groupName = group.groupName
                        )
                    )
                }

            }

            UserSaveSource.ADMIN_UPDATE -> {
                handleGroup(userId, req.groupId)
                // explicitly NO EMAIL
                emailVerified = true
            }
        }
        return SaveOrUpdateUserResponse(
            userId = userId,
            emailVerified = emailVerified,
            emailOtpExpiresAtMillis = expiresOtpAtMillis
        )
    }

    fun getCurrentDate(): String {
        return LocalDate.now().toString()
    }

    fun addUserRole(userId: Int, roleCode: String): Int {

        val roleId = rolesSql.getRoleIdByRoleCode(roleCode)
            ?: throw IllegalArgumentException("Invalid roleCode=$roleCode")

        val existingRoleIds = userRolesSql.getAllUserRoleIds(userId)

        if (existingRoleIds.contains(roleId)) {
            // Role already assigned → do nothing
            return roleId
        }

        return userRolesSql.addUserRole(
            userId = userId,
            roleId = roleId,
            status = "ACTIVE"
        )
    }

    fun handleGroup(userId: Int, groupId: Int) {
        if (groupId > 0) {
            // Get group member and if not present add.

            val groupMember = usersSql.getGroupMember(userId, groupId)
            if (groupMember.isEmpty()) {
                groupsSql.addGroupMember(userId, groupId, getCurrentDate())
            }
        }
    }

    fun changePassword(userId: Int, rawPassword: String) {
        val lastHashes = passwordHistSql.getLast3PasswordHashes(userId)

        val reused = lastHashes.any { storedHash ->
            Converters.verifyPassword(rawPassword, storedHash)
        }

        if (reused) {
            throw ValidationException(
                errorCode = "PASSWORD_REUSED",
                message = "Cannot reuse last 3 passwords"
            )
        }

        val newHash = Converters.hashPassword(rawPassword)

        usersSql.updatePassword(userId, newHash)
    }
}
