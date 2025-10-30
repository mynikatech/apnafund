package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.data.model.UserPasswordHistory
import com.mynikatech.apnafund.data.model.UserWithGroup
import com.mynikatech.apnafund.data.model.Users
import kotlinx.coroutines.flow.Flow

@Dao
interface UsersDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addUser(user: Users)

    @Query("SELECT * from users")
    fun getAllUsers(): Flow<List<Users>>

    @Query("SELECT * from users")
    suspend fun getUsers(): List<Users>

    @Query("SELECT * from users WHERE userId = :userId")
    suspend fun getUser(userId: Int): Users?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: Users)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(users: List<Users>)

    @Query("""SELECT CAST(COUNT(1) AS BIT)
            FROM users
            WHERE emailId = :emailId""")
    suspend fun doesUserExists(emailId: String): Boolean

    @Query("SELECT COUNT(*) FROM users WHERE (emailId = :email OR phoneNumber = :phone) AND userId != :excludeUserId")
    suspend fun countMatchingUsers(email: String, phone: String, excludeUserId: Int): Int

    @Update
    suspend fun updateUser(user: Users)

    @Query("DELETE FROM users")
    suspend fun deleteAll()

    @Delete
    suspend fun deleteUser(user: Users)

    @Transaction
    suspend fun replaceAll(users: List<Users>) {
        deleteAll()
        upsertAll(users)
    }

    @Query("UPDATE USERS SET passwordHash = :password WHERE userId = :userId")
    suspend fun updateUserPassword(userId: Int, password: String)

    @Query("UPDATE USERS SET hashPIN = :pin, isPinSet = '1' WHERE userId = :userId")
    suspend fun updateUserPIN(userId: Int, pin: String)

    @Query("SELECT * FROM users WHERE emailId = :email AND passwordHash = :password LIMIT 1")
    suspend fun validateUser(email: String, password: String): Users?

    @Query("SELECT * FROM users WHERE emailId = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): Users?

    @Query("SELECT * FROM users WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): Users?

    @Query("SELECT CAST(COUNT(1) AS BIT)\n" +
            "            FROM users\n" +
            "            WHERE hashPIN = :pin AND userId = :userId")
    suspend fun checkUserPIN(userId: Int, pin: String): Boolean

    @Insert
    suspend fun insertHistory(entry: UserPasswordHistory)

    @Transaction
    suspend fun createUserAndReturnId(user: Users): Int {
        addUser(user)

        val email = user.emailId ?: throw IllegalArgumentException("Email cannot be null")

        val insertedUser = getUserByEmail(email)
            ?: throw IllegalStateException("User not found after insert for email: $email")
        val hash = user.passwordHash
        if(hash!= null) {
            insertHistory(UserPasswordHistory(userId = insertedUser.userId, passwordHash = hash))
        }
        return insertedUser.userId
    }

    @Query("""
    SELECT 
        u.userId AS userId,
        u.firstName AS firstName,
        u.lastName AS lastName,
        u.emailId AS emailId,
        u.phoneNumber AS phoneNumber,
        u.status AS status,
        u.userCode AS userCode,
        u.isPinSet as isPinSet,
        g.groupId AS groupId,
        g.groupName AS groupName,
        g.moderator AS moderator,
        g.description AS description,
        g.status AS groupStatus

    FROM users u
    LEFT JOIN group_members gm ON u.userId = gm.userId
    LEFT JOIN 'groups' g ON gm.groupId = g.groupId
    WHERE g.groupId = :groupId
""")
    fun getUserWithGroup(groupId: Int): Flow<List<UserWithGroup>>
    @Query("""
    SELECT 
        u.userId AS userId,
        u.firstName AS firstName,
        u.lastName AS lastName,
        u.emailId AS emailId,
        u.phoneNumber AS phoneNumber,
        u.status AS status,
        u.userCode AS userCode,
        u.isPinSet as isPinSet,
        g.groupId AS groupId,
        g.groupName AS groupName,
        g.moderator AS moderator,
        g.description AS description,
        g.status AS groupStatus

    FROM users u
    LEFT JOIN group_members gm ON u.userId = gm.userId
    LEFT JOIN 'groups' g ON gm.groupId = g.groupId
    """)
    fun getAllUsersWithGroup(): Flow<List<UserWithGroup>>

    @Insert
    suspend fun addGroupMember(groupMembers: GroupMembers)

    @Update
    suspend fun updateGroupMember(groupMembers: GroupMembers)

    @Query(" SELECT * from GROUP_MEMBERS WHERE userId =:userId AND groupId = :groupId")
    suspend fun getGroupMember(userId: Int, groupId: Int): GroupMembers?

    @Query("""
    SELECT EXISTS (
        SELECT 1
        FROM user_roles ur
        JOIN roles r ON ur.roleId = r.roleId
        JOIN group_members gm ON ur.userId = gm.userId
        WHERE gm.groupId = :groupId
          AND r.roleCode = 'MODERATOR'
          AND ur.status = 'ACTIVE'
        LIMIT 1
    )
""")
    suspend fun doesGroupHasModerator(groupId: Int): Boolean
}