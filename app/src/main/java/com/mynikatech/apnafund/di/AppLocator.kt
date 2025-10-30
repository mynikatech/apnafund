package com.mynikatech.apnafund.di

import android.content.Context
import com.mynikatech.apnafund.BuildConfig
import com.mynikatech.apnafund.data.dao.AdminDao
import com.mynikatech.apnafund.data.dao.DepositsDao
import com.mynikatech.apnafund.data.dao.FeedbackDao
import com.mynikatech.apnafund.data.dao.FundMembersDao
import com.mynikatech.apnafund.data.dao.FundsDao
import com.mynikatech.apnafund.data.dao.GroupMembersDao
import com.mynikatech.apnafund.data.dao.GroupsDao
import com.mynikatech.apnafund.data.dao.PasswordHistoryDao
import com.mynikatech.apnafund.data.dao.PinHistoryDao
import com.mynikatech.apnafund.data.dao.RolesDao
import com.mynikatech.apnafund.data.dao.UserRolesDao
import com.mynikatech.apnafund.data.dao.UsersDao
import com.mynikatech.apnafund.data.database.ApnaFundDatabase
import com.mynikatech.apnafund.data.repository.DepositRepository
import com.mynikatech.apnafund.data.repository.FundRepository
import com.mynikatech.apnafund.data.repository.GroupRepository
import com.mynikatech.apnafund.data.repository.UserRoleRepository
import com.mynikatech.apnafund.net.AdminApi
import com.mynikatech.apnafund.net.AdminApiKtor
import com.mynikatech.apnafund.net.DepositsApi
import com.mynikatech.apnafund.net.DepositsApiKtor
import com.mynikatech.apnafund.net.FundsApi
import com.mynikatech.apnafund.net.FundsApiKtor
import com.mynikatech.apnafund.net.GroupsApi
import com.mynikatech.apnafund.net.GroupsApiKtor
import com.mynikatech.apnafund.net.LoansApi
import com.mynikatech.apnafund.net.LoansApiKtor
import com.mynikatech.apnafund.net.PasswordHistoryApi
import com.mynikatech.apnafund.net.PasswordHistoryApiKtor
import com.mynikatech.apnafund.net.PinHistoryApi
import com.mynikatech.apnafund.net.PinHistoryApiKtor
import com.mynikatech.apnafund.net.RolesApi
import com.mynikatech.apnafund.net.RolesApiKtor
import com.mynikatech.apnafund.net.UserRolesApi
import com.mynikatech.apnafund.net.UserRolesApiKtor
import com.mynikatech.apnafund.net.UsersApi
import com.mynikatech.apnafund.net.UsersApiKtor
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object AppLocator {

    @Volatile
    private var initialized = false
    fun init(appContext: Context) {
        if (initialized) return
        // Room
        db = ApnaFundDatabase.getDatabase(appContext)

        // DAOs
        usersDao = db.getUsersDao()
        rolesDao = db.getRolesDao()
        userRolesDao = db.getUserRolesDao()
        passwordHistoryDao = db.getPasswordHistoryDao()
        adminDao = db.getAdminDao()
        pinHistoryDao = db.getPinHistoryDao()
        groupsDao = db.getGroupsDao()
        groupMembersDao = db.getGroupMembersDao()
        depositsDao = db.getDepositsDao()
        fundsDao = db.getFundsDao()
        fundMembersDao = db.getFundMembersDao()
        feedbackDao = db.getFeedbackDao()

        initialized = true
    }

    private fun requireInit() {
        check(initialized) {
            "AppLocator.init(context) was not called. Initialize in Application.onCreate()."
        }
    }


    /* -------- Room -------- */
    lateinit var db: ApnaFundDatabase
        private set

    lateinit var usersDao: UsersDao
        private set
    lateinit var rolesDao: RolesDao
        private set
    lateinit var userRolesDao: UserRolesDao
        private set
    lateinit var passwordHistoryDao: PasswordHistoryDao
        private set
    lateinit var adminDao: AdminDao
        private set
    lateinit var pinHistoryDao: PinHistoryDao
        private set
    lateinit var groupsDao: GroupsDao
        private set
    lateinit var groupMembersDao: GroupMembersDao
        private set
    lateinit var depositsDao: DepositsDao
        private set
    lateinit var fundsDao: FundsDao
        private set
    lateinit var fundMembersDao: FundMembersDao
        private set

    lateinit var feedbackDao: FeedbackDao
        private set

    /* -------- Ktor HttpClient -------- */
    val httpClient: HttpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                        explicitNulls = false
                    }
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.INFO
            }
            // Use BuildConfig.SERVER_BASE_URL set in app build.gradle.kts
            defaultRequest {
                url(BuildConfig.SERVER_BASE_URL)   // e.g. http://10.0.2.2:8080
                contentType(ContentType.Application.Json)
            }
        }
    }

    /* -------- APIs (Ktor) -------- */
    val usersApi: UsersApi by lazy { UsersApiKtor { httpClient } }
    val groupsApi: GroupsApi by lazy { GroupsApiKtor { httpClient } }
    val fundsApi: FundsApi by lazy { FundsApiKtor { httpClient } }
    val rolesApi: RolesApi by lazy { RolesApiKtor { httpClient } }
    val userRolesApi: UserRolesApi by lazy { UserRolesApiKtor { httpClient } }
    val depositsApi: DepositsApi by lazy { DepositsApiKtor { httpClient } }
    val pinHistoryApi: PinHistoryApi by lazy { PinHistoryApiKtor { httpClient } }
    val passwordHistoryApi: PasswordHistoryApi by lazy { PasswordHistoryApiKtor { httpClient } }
    val loansApi: LoansApi by lazy { LoansApiKtor { httpClient } }
    val adminApi: AdminApi by lazy { AdminApiKtor { httpClient } }
    // add more as you implement (LoansApi, RolesApi, etc.)

    /* -------- Repositories -------- */
    val userRolesRepository: UserRoleRepository by lazy {
        requireInit()
        UserRoleRepository(
            usersApi = usersApi,
            rolesApi = rolesApi,
            groupsApi = groupsApi,
            userRolesApi = userRolesApi,
            pinHistoryApi = pinHistoryApi,
            passwordHistoryApi = passwordHistoryApi,
            adminApi = adminApi
        )
    }

    val groupRepository: GroupRepository by lazy {
        GroupRepository(groupsDao, groupMembersDao, groupsApi)
    }

    val depositRepository: DepositRepository by lazy {
        DepositRepository(depositDao = depositsDao, api = depositsApi)
    }

    // Add funds/loans repositories as needed…
    val fundRepository: FundRepository by lazy {
        FundRepository(fundsDao, fundMembersDao, fundsApi)
    }
}
