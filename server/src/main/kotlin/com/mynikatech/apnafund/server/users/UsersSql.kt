import com.mynikatech.apnafund.net.dto.FeedbackDto
import com.mynikatech.apnafund.net.dto.FeedbackWithUserGroupDto
import com.mynikatech.apnafund.net.dto.FundsDto
import com.mynikatech.apnafund.net.dto.GroupMembersDto
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.UserDetailsDto
import com.mynikatech.apnafund.net.dto.UserFundDetailsDto
import com.mynikatech.apnafund.net.dto.UserProfileDto
import com.mynikatech.apnafund.net.dto.UserWithGroupDto
import com.mynikatech.apnafund.net.dto.UsersDto
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

@RegisterKotlinMapper(UsersDto::class)
@RegisterKotlinMapper(UserWithGroupDto::class)
@RegisterKotlinMapper(GroupsDto::class)
@RegisterKotlinMapper(FundsDto::class)
@RegisterKotlinMapper(UserDetailsDto::class)
@RegisterKotlinMapper(UserFundDetailsDto::class)
@RegisterKotlinMapper(GroupMembersDto::class)
@RegisterKotlinMapper(FeedbackDto::class)
@RegisterKotlinMapper(FeedbackWithUserGroupDto::class)
@RegisterKotlinMapper(UserProfileDto::class)
interface UsersSql {

    @SqlQuery("""SELECT * FROM get_users()""")
    fun getUsers(): List<UsersDto>

    @SqlQuery("""SELECT * FROM get_user(:id)""")
    fun getUser(@Bind("id") id: Int): List<UsersDto>

    @SqlQuery(
        """
        SELECT upsert_user_by_email(:firstName,:lastName,:emailId,:phoneNumber,:status,
                                    :createdDate,:isPinSet,:hashPIN,:passwordHash,:firebaseUserId,:userCode)
    """
    )
    fun upsertUserByEmail(@BindKotlin u: UsersDto): Int

    @SqlUpdate("""CALL delete_user(:id)""")
    fun deleteUser(@Bind("id") userId: Int)

    @SqlUpdate("""CALL delete_all_users()""")
    fun deleteAll()

    @SqlQuery("""SELECT * FROM get_user_by_email(:email)""")
    fun getUserByEmail(@Bind("email") email: String): List<UsersDto>

    @SqlQuery("""SELECT * FROM get_user_by_phone(:phone)""")
    fun getUserByPhone(@Bind("phone") phone: String): List<UsersDto>

    @SqlQuery("""SELECT * FROM get_user_with_group(:groupId)""")
    fun getUserWithGroup(@Bind("groupId") groupId: Int): List<UserWithGroupDto>

    @SqlQuery("""SELECT * FROM get_all_users_with_group()""")
    fun getAllUsersWithGroup(): List<UserWithGroupDto>

    @SqlUpdate("""CALL update_user_password(:id,:hash)""")
    fun updatePassword(@Bind("id") userId: Int, @Bind("hash") passwordHash: String)

    @SqlUpdate("""CALL update_user_pin(:id,:pin)""")
    fun updatePin(@Bind("id") userId: Int, @Bind("pin") pinHash: String)

    @SqlQuery("""SELECT does_user_exist(:email)""")
    fun doesUserExists(@Bind("email") email: String): Boolean

    @SqlQuery("""SELECT count_matching_users(:email,:phone,:excludeId)""")
    fun countMatchingUsers(
        @Bind("email") email: String,
        @Bind("phone") phone: String,
        @Bind("excludeId") excludeUserId: Int
    ): Int

    @SqlQuery("""SELECT check_user_pin(:id,:pin)""")
    fun checkUserPIN(@Bind("id") userId: Int, @Bind("pin") pin: String): Boolean

    @SqlQuery("""SELECT does_group_have_moderator(:groupId)""")
    fun doesGroupHasModerator(@Bind("groupId") groupId: Int): Boolean

    @SqlUpdate("""CALL upsert_group_member(:uid,:gid,:joined)""")
    fun upsertGroupMember(
        @Bind("uid") userId: Int,
        @Bind("gid") groupId: Int,
        @Bind("joined") joiningDate: java.time.LocalDate
    )

    @SqlQuery("""SELECT * FROM get_group_member(:userId, :groupId)""")
    fun getGroupMember(
        @Bind("userId") userId: Int,
        @Bind("groupId") groupId: Int
    ): List<GroupMembersDto>

    @SqlUpdate(
        """
        INSERT INTO feedbacks("userId","message","timestamp")
        VALUES (:userId,:message,:timestamp)
    """
    )
    fun insertFeedback(@BindKotlin f: FeedbackDto): Int

    @SqlQuery(
        """
        SELECT "feedbackId","userId","message","timestamp"
        FROM feedbacks
        ORDER BY "timestamp" DESC
    """
    )
    fun getAllFeedbacks(): List<FeedbackDto>

    @SqlQuery("""SELECT * FROM get_feedback_with_user_group()""")
    fun getFeedbackWithUserGroup(): List<FeedbackWithUserGroupDto>

    @SqlQuery("""SELECT * FROM get_user_profile(:userId)""")
    fun getUserProfile(@Bind("userId") userId: Int): List<UserProfileDto>

    // ---- NEW: aggregates / extras ----
    @SqlQuery("""SELECT * FROM get_group_for_user(:userId)""")
    fun getGroupForUser(@Bind("userId") userId: Int): List<GroupsDto>

    @SqlQuery("""SELECT * FROM get_funds_for_user(:userId)""")
    fun getFundsForUser(@Bind("userId") userId: Int): List<FundsDto>

    @SqlQuery("""SELECT get_total_deposit(:userId,:fundId)""")
    fun getTotalDeposit(@Bind("userId") userId: Int, @Bind("fundId") fundId: Int): Double?

    @SqlQuery("""SELECT get_per_member_expected_maturity_amount(:fundId)""")
    fun getPerMemberExpectedMaturityAmount(@Bind("fundId") fundId: Int): Double?

    @SqlQuery("""SELECT get_total_loan_amount(:userId,:fundId)""")
    fun getTotalLoanAmount(@Bind("userId") userId: Int, @Bind("fundId") fundId: Int): Double

    @SqlQuery("""SELECT * FROM get_user_details(:userId)""")
    fun getUserDetails(@Bind("userId") userId: Int): List<UserDetailsDto>

    @SqlQuery("""SELECT get_user_fund_details_json(:userId, :fundId)::text""")
    fun getUserFundDetails(
        @Bind("userId") userId: Int,
        @Bind("fundId") fundId: Int
    ): String?

    @SqlQuery("""SELECT * FROM validate_user(:email, :pwd)""")
    fun validateUser(
        @Bind("email") email: String,
        @Bind("pwd")   passwordHash: String
    ): UsersDto?
}
