package com.mynikatech.apnafund.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mynikatech.apnafund.data.dao.AdminDao
import com.mynikatech.apnafund.data.dao.DepositsDao
import com.mynikatech.apnafund.data.dao.FeedbackDao
import com.mynikatech.apnafund.data.dao.FundMembersDao
import com.mynikatech.apnafund.data.dao.FundsDao
import com.mynikatech.apnafund.data.dao.GroupMembersDao
import com.mynikatech.apnafund.data.dao.GroupsDao
import com.mynikatech.apnafund.data.dao.LoanDetailsDao
import com.mynikatech.apnafund.data.dao.LoanEmisDao
import com.mynikatech.apnafund.data.dao.LoansDao
import com.mynikatech.apnafund.data.dao.NotificationsDao
import com.mynikatech.apnafund.data.dao.PasswordHistoryDao
import com.mynikatech.apnafund.data.dao.PinHistoryDao
import com.mynikatech.apnafund.data.dao.RolesDao
import com.mynikatech.apnafund.data.dao.TypeDao
import com.mynikatech.apnafund.data.dao.UserRolesDao
import com.mynikatech.apnafund.data.dao.UserSummaryDao
import com.mynikatech.apnafund.data.dao.UsersDao
import com.mynikatech.apnafund.data.model.Deposits
import com.mynikatech.apnafund.data.model.Feedback
import com.mynikatech.apnafund.data.model.FundDetails
import com.mynikatech.apnafund.data.model.FundMembers
import com.mynikatech.apnafund.data.model.Funds
import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.data.model.Groups
import com.mynikatech.apnafund.data.model.LoanDetails
import com.mynikatech.apnafund.data.model.LoanEmis
import com.mynikatech.apnafund.data.model.Loans
import com.mynikatech.apnafund.data.model.Privilege
import com.mynikatech.apnafund.data.model.RolePrivilege
import com.mynikatech.apnafund.data.model.Roles
import com.mynikatech.apnafund.data.model.Type
import com.mynikatech.apnafund.data.model.UserNotifications
import com.mynikatech.apnafund.data.model.UserPasswordHistory
import com.mynikatech.apnafund.data.model.UserPinHistory
import com.mynikatech.apnafund.data.model.UserRoles
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.util.Converters

@Database(
    entities =
        [
            Users::class,
            Roles::class,
            UserRoles::class,
            Groups::class,
            GroupMembers::class,
            Funds::class,
            Deposits::class,
            Loans::class,
            LoanDetails::class,
            LoanEmis::class,
            FundDetails::class,
            Type::class,
            Privilege::class,
            RolePrivilege::class,
            UserNotifications::class,
            UserPasswordHistory::class,
            UserPinHistory::class,
            Feedback::class,
            FundMembers::class
        ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ApnaFundDatabase : RoomDatabase() {

    abstract fun getUsersDao(): UsersDao
    abstract fun getRolesDao(): RolesDao
    abstract fun getUserRolesDao(): UserRolesDao
    abstract fun getLoansDao(): LoansDao
    abstract fun getGroupsDao(): GroupsDao
    abstract fun getDepositsDao(): DepositsDao
    abstract fun getLoanDetailsDao(): LoanDetailsDao
    abstract fun getFundsDao(): FundsDao
    abstract fun getLoanEmisDao(): LoanEmisDao
    abstract fun getGroupMembersDao(): GroupMembersDao
    abstract fun getUserSummaryDao(): UserSummaryDao
    abstract fun getTypeDao(): TypeDao
    abstract fun getNotificationsDao(): NotificationsDao
    abstract fun getPasswordHistoryDao(): PasswordHistoryDao
    abstract fun getAdminDao(): AdminDao
    abstract fun getPinHistoryDao(): PinHistoryDao
    abstract fun getFeedbackDao(): FeedbackDao
    abstract fun getFundMembersDao(): FundMembersDao

    companion object {

        private var DATABASE_INSTANCE: ApnaFundDatabase? = null

        private val dbCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val insertUser = """
                                INSERT INTO USERS (
                                userId, firstName, lastName, emailId, phoneNumber, status,
                                createdDate, userCode, isPinSet
                                 ) VALUES (
                                 1, 'Sunil', 'Agarwal', 'sunilagl@gmail.com', '9004533807', 'ACTIVE', '16-07-2025', 'SAAG123456', 0
                                    );
                                """.trimIndent()

                val insertUserRoles = """
                                INSERT INTO USER_ROLES (userRoleId, userId, roleId, status) VALUES 
                                (1, 1, 1, 'ACTIVE'),
                                (2, 1, 2, 'ACTIVE'),
                                (3, 1, 3, 'ACTIVE');
                                """.trimIndent()

                //ROLES
                db.execSQL("INSERT INTO ROLES VALUES (1, 'ADMIN', 'Perform Admin Functions', 'ACTIVE')")
                db.execSQL("INSERT INTO ROLES VALUES (2, 'MODERATOR', 'Coordinates Group Activities and manages Fund', 'ACTIVE')")
                db.execSQL("INSERT INTO ROLES VALUES (3, 'MEMBER', 'A regular Fund Member', 'ACTIVE')")
                // TYPES
                db.execSQL("INSERT INTO TYPE VALUES (1, 'DEPOVERDUE', 'Your Deposit is Overdue', 'ACTIVE')")
                db.execSQL("INSERT INTO TYPE VALUES (2, 'EMIOVERDUE', 'Your EMI is Overdue', 'ACTIVE')")
                db.execSQL("INSERT INTO TYPE VALUES (3, 'EMIDUE', 'Your EMI is due', 'ACTIVE')")
                db.execSQL("INSERT INTO TYPE VALUES (4, 'DEPDUE', 'Your Deposit is due', 'ACTIVE')")

                // PRIVILEGE
                db.execSQL("INSERT INTO PRIVILEGE VALUES (1, 'ADDUSER', 'Privilege to add/edit a user', 'ACTIVE')")
                db.execSQL("INSERT INTO PRIVILEGE VALUES (2, 'ADDGROUP', 'Privilege to add/edit a group', 'ACTIVE')")
                db.execSQL("INSERT INTO PRIVILEGE VALUES (3, 'ADDFUND', 'Privilege to add/edit a fund', 'ACTIVE')")
                db.execSQL("INSERT INTO PRIVILEGE VALUES (4, 'APPLYLOAN', 'Privilege to apply a loan', 'ACTIVE')")
                db.execSQL("INSERT INTO PRIVILEGE VALUES (5, 'APPROVELOAN', 'Privilege to approve a loan', 'ACTIVE')")
                db.execSQL("INSERT INTO PRIVILEGE VALUES (6, 'ADDGROUPMEMBER', 'Privilege to  add member to a group', 'ACTIVE')")
                db.execSQL("INSERT INTO PRIVILEGE VALUES (7, 'ADDFUNDMEMBER', 'Privilege to add member to a fund', 'ACTIVE')")

                // ROLE_PRIVILEGE
                db.execSQL("INSERT INTO ROLE_PRIVILEGE VALUES (1, 1, 1, 'ACTIVE')")
                db.execSQL("INSERT INTO ROLE_PRIVILEGE VALUES (2, 2, 1, 'ACTIVE')")
                db.execSQL("INSERT INTO ROLE_PRIVILEGE VALUES (3, 3, 1, 'ACTIVE')")
                db.execSQL("INSERT INTO ROLE_PRIVILEGE VALUES (4, 3, 2, 'ACTIVE')")
                db.execSQL("INSERT INTO ROLE_PRIVILEGE VALUES (5, 5, 1, 'ACTIVE')")
                db.execSQL("INSERT INTO ROLE_PRIVILEGE VALUES (6, 4, 3, 'ACTIVE')")
                db.execSQL("INSERT INTO ROLE_PRIVILEGE VALUES (7, 2, 2, 'ACTIVE')")
                db.execSQL("INSERT INTO ROLE_PRIVILEGE VALUES (8, 6, 1, 'ACTIVE')")
                db.execSQL("INSERT INTO ROLE_PRIVILEGE VALUES (9, 1, 2, 'ACTIVE')")
                db.execSQL("INSERT INTO ROLE_PRIVILEGE VALUES (10, 4, 2, 'ACTIVE')")
                db.execSQL("INSERT INTO ROLE_PRIVILEGE VALUES (11, 5, 2, 'ACTIVE')")
                db.execSQL("INSERT INTO ROLE_PRIVILEGE VALUES (12, 7, 2, 'ACTIVE')")
                db.execSQL("INSERT INTO ROLE_PRIVILEGE VALUES (13, 7, 1, 'ACTIVE')")


                db.execSQL(insertUser)
                db.execSQL(insertUserRoles)
            }
        }

        fun getDatabase(context: Context): ApnaFundDatabase {

            return DATABASE_INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    ApnaFundDatabase::class.java,
                    "apna-bank-database"

                ).fallbackToDestructiveMigration()
                    .addCallback(dbCallback)
                    .build()
                DATABASE_INSTANCE = instance
                return instance
            }
        }
    }

}