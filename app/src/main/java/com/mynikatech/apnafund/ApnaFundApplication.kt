package com.mynikatech.apnafund

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.mynikatech.apnafund.data.database.ApnaFundDatabase
import com.mynikatech.apnafund.data.repository.AIRepository
import com.mynikatech.apnafund.data.repository.ApprovalRepository
import com.mynikatech.apnafund.data.repository.DepositRepository
import com.mynikatech.apnafund.data.repository.FeedbackRepository
import com.mynikatech.apnafund.data.repository.FundRepository
import com.mynikatech.apnafund.data.repository.GroupRepository
import com.mynikatech.apnafund.data.repository.LoanRepository
import com.mynikatech.apnafund.data.repository.TypeRepository
import com.mynikatech.apnafund.data.repository.UserRoleRepository
import com.mynikatech.apnafund.data.repository.UserSummaryRepository
import com.mynikatech.apnafund.di.AppLocator
import com.mynikatech.apnafund.net.AIApiKtor
import com.mynikatech.apnafund.net.AdminApiKtor
import com.mynikatech.apnafund.net.ApprovalApiKtor
import com.mynikatech.apnafund.net.DepositsApiKtor
import com.mynikatech.apnafund.net.FeedbackApiKtor
import com.mynikatech.apnafund.net.FundsApiKtor
import com.mynikatech.apnafund.net.GroupsApiKtor
import com.mynikatech.apnafund.net.LoansApiKtor
import com.mynikatech.apnafund.net.NotificationsApiKtor
import com.mynikatech.apnafund.net.PasswordHistoryApiKtor
import com.mynikatech.apnafund.net.PinHistoryApiKtor
import com.mynikatech.apnafund.net.RolesApiKtor
import com.mynikatech.apnafund.net.TypeApiKtor
import com.mynikatech.apnafund.net.UserRolesApiKtor
import com.mynikatech.apnafund.net.UsersApiKtor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ApnaFundApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val database = ApnaFundDatabase.getDatabase(this)
        val fundDao = database.getFundsDao()
        val depositsDao = database.getDepositsDao()
        val loansDao = database.getLoansDao()
        val loanEmisDao = database.getLoanEmisDao()
        val typeDao = database.getTypeDao()
        val fundMembersDao = database.getFundMembersDao()
        val feedbackDao = database.getFeedbackDao()
        val groupsApi = GroupsApiKtor()
        val fundApi = FundsApiKtor()
        val userApi = UsersApiKtor()
        val feedbackApi = FeedbackApiKtor()
        val userRolesApi = UserRolesApiKtor()
        val rolesApi = RolesApiKtor()
        val adminApi = AdminApiKtor()
        val typeApi = TypeApiKtor()
        val loansApi = LoansApiKtor()
        val depositsApi = DepositsApiKtor()
        val pinHistoryApi = PinHistoryApiKtor()
        val passwordHistoryApi = PasswordHistoryApiKtor()
        val notificationsApi = NotificationsApiKtor()
        val approvalApi = ApprovalApiKtor()
        val aIApi = AIApiKtor()
        val firestore = FirebaseFirestore.getInstance()

        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                MemoryCacheSettings.newBuilder().build()
            )
            .build()

        firestore.firestoreSettings = settings
        AppLocator.init(applicationContext)

        userRolesRepository = UserRoleRepository(
            userApi,
            rolesApi,
            groupsApi,
            userRolesApi,
            pinHistoryApi,
            passwordHistoryApi,
            adminApi
        )
        fundRepository = FundRepository(fundDao, fundMembersDao, api = fundApi)
        groupRepository = GroupRepository(groupsApi)
        depositRepository = DepositRepository(depositsDao, depositsApi)
        typeRepository = TypeRepository(typeDao, typeApi)
        loanRepository = LoanRepository(loansDao, loanEmisDao, loansApi)
        feedbackRepository = FeedbackRepository(feedbackDao, feedbackApi)
        approvalRepository = ApprovalRepository(approvalApi)
        aiRepository = AIRepository(aIApi)
        userSummaryRepository = UserSummaryRepository(
            userApi,
            loansApi,
            userRolesApi,
            fundApi,
            notificationsApi

        )
        CoroutineScope(Dispatchers.IO).launch {
            rolePrivilegesMap = userRolesRepository.getPrivilegesGroupedByRole()
            rolesMap = userRolesRepository.getRoleCodesByRoleId()
            typeMap = typeRepository.getAllTypes()
        }

    }

    companion object {

        lateinit var userRolesRepository: UserRoleRepository
        lateinit var fundRepository: FundRepository
        lateinit var groupRepository: GroupRepository
        lateinit var depositRepository: DepositRepository
        lateinit var userSummaryRepository: UserSummaryRepository
        lateinit var loanRepository: LoanRepository
        lateinit var feedbackRepository: FeedbackRepository
        lateinit var rolePrivilegesMap: Map<Int, List<String>>
        lateinit var typeRepository: TypeRepository
        lateinit var typeMap: Map<String, String>
        lateinit var rolesMap: Map<String, Int>
        lateinit var approvalRepository: ApprovalRepository
        lateinit var aiRepository: AIRepository


    }
}