package com.mynikatech.apnafund.server

import UsersSql
import com.mynikatech.apnafund.server.admin.AdminSql
import com.mynikatech.apnafund.server.admin.adminRoutes
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

fun Application.registerRoutes(
    usersDao: UsersSql, deposistsDao: DepositsSql,
    feedbackDao: FeedbackSql, groupsDao: GroupsSql,
    roleDao: RolesSql, userRolesDao: UserRolesSql,
    fundDao: FundsSql, loansDao: LoansSql,
    typeDao: TypesSql, notificationsDao: NotificationsSql,
    adminDao: AdminSql, pinHistoryDao: PinHistorySql,
    passwordHistoryDao: PasswordHistorySql
) {
    routing {
        usersRoutes(usersDao)
        groupsRoutes(groupsDao)
        rolesRoutes(roleDao)
        userRolesRoutes(userRolesDao)
        fundsRoutes(fundDao)
        depositsRoutes(deposistsDao)
        loansRoutes(loansDao)
        typesRoutes(typeDao)
        notificationsRoutes(notificationsDao)
        feedbackRoutes(feedbackDao)
        adminRoutes(adminDao)
        pinHistoryRoutes(pinHistoryDao)
        passwordHistoryRoutes(passwordHistoryDao)
        rolePrivilegeRoutes(roleDao)
        privilegesRoutes(roleDao)
        userDetailsRoute(usersDao,userRolesDao,notificationsDao)
    }
}
