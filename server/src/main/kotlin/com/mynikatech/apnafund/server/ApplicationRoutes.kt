package com.mynikatech.apnafund.server

import com.mynikatech.apnafund.server.admin.AdminSql
import com.mynikatech.apnafund.server.admin.adminRoutes
import com.mynikatech.apnafund.server.ai.AIService
import com.mynikatech.apnafund.server.ai.aiRoutes
import com.mynikatech.apnafund.server.approval.ApprovalService
import com.mynikatech.apnafund.server.approval.ApprovalSql
import com.mynikatech.apnafund.server.approval.approvalRoutes
import com.mynikatech.apnafund.server.deposits.DepositsSql
import com.mynikatech.apnafund.server.deposits.depositsRoutes
import com.mynikatech.apnafund.server.feedback.FeedbackSql
import com.mynikatech.apnafund.server.feedback.feedbackRoutes
import com.mynikatech.apnafund.server.funds.FundsSql
import com.mynikatech.apnafund.server.funds.fundsRoutes
import com.mynikatech.apnafund.server.groups.GroupsSql
import com.mynikatech.apnafund.server.groups.groupsRoutes
import com.mynikatech.apnafund.server.loans.LoansSql
import com.mynikatech.apnafund.server.loans.loansRoutes
import com.mynikatech.apnafund.server.notifications.NotificationsSql
import com.mynikatech.apnafund.server.notifications.notificationsRoutes
import com.mynikatech.apnafund.server.pin.pinHistoryRoutes
import com.mynikatech.apnafund.server.roles.RolesSql
import com.mynikatech.apnafund.server.roles.privilegesRoutes
import com.mynikatech.apnafund.server.roles.rolePrivilegeRoutes
import com.mynikatech.apnafund.server.roles.rolesRoutes
import com.mynikatech.apnafund.server.security.PasswordHistorySql
import com.mynikatech.apnafund.server.security.PinHistorySql
import com.mynikatech.apnafund.server.security.passwordHistoryRoutes
import com.mynikatech.apnafund.server.types.TypesSql
import com.mynikatech.apnafund.server.types.typesRoutes
import com.mynikatech.apnafund.server.userroles.UserRolesSql
import com.mynikatech.apnafund.server.userroles.userRolesRoutes
import com.mynikatech.apnafund.server.users.userDetailsRoute
import com.mynikatech.apnafund.server.users.usersRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import com.mynikatech.apnafund.server.common.messaging.dispatch.EventDispatchService
import com.mynikatech.apnafund.server.notifications.NotificationService
import com.mynikatech.apnafund.server.users.EmailVerificationService
import com.mynikatech.apnafund.server.users.ModeratorRegistrationService
import com.mynikatech.apnafund.server.users.OtpService
import com.mynikatech.apnafund.server.users.UserManagementService
import com.mynikatech.apnafund.server.users.UsersSql

fun Application.registerRoutes(
    usersDao: UsersSql, deposistsDao: DepositsSql,
    feedbackDao: FeedbackSql, groupsDao: GroupsSql,
    roleDao: RolesSql, userRolesDao: UserRolesSql,
    fundDao: FundsSql, loansDao: LoansSql,
    typeDao: TypesSql, notificationsDao: NotificationsSql,
    adminDao: AdminSql, pinHistoryDao: PinHistorySql,
    passwordHistoryDao: PasswordHistorySql,
    eventDispatchService: EventDispatchService,
    moderatorRegistrationService: ModeratorRegistrationService,
    userManagementService: UserManagementService,
    emailVerificationService: EmailVerificationService,
    notificationService: NotificationService,
    approvalDao: ApprovalSql, approvalService: ApprovalService,
    aiService: AIService, otpService: OtpService
) {
    routing {
        usersRoutes(usersDao,eventDispatchService, moderatorRegistrationService,userManagementService,emailVerificationService, userRolesDao, otpService)
        groupsRoutes(groupsDao,eventDispatchService,usersDao,notificationService,approvalDao)
        rolesRoutes(roleDao)
        userRolesRoutes(userRolesDao, roleDao, typeDao)
        fundsRoutes(fundDao,notificationService,usersDao )
        depositsRoutes(deposistsDao)
        loansRoutes(loansDao,eventDispatchService,usersDao, fundDao, notificationService,approvalDao)
        typesRoutes(typeDao)
        notificationsRoutes(notificationsDao)
        feedbackRoutes(feedbackDao)
        adminRoutes(adminDao,eventDispatchService, usersDao)
        pinHistoryRoutes(pinHistoryDao)
        passwordHistoryRoutes(passwordHistoryDao)
        rolePrivilegeRoutes(roleDao)
        privilegesRoutes(roleDao)
        userDetailsRoute(usersDao,userRolesDao,notificationsDao)
        approvalRoutes(approvalDao, approvalService)
        aiRoutes(aiService)
    }
}
