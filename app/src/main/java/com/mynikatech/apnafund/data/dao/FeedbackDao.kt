package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mynikatech.apnafund.data.model.Feedback
import com.mynikatech.apnafund.data.model.FeedbackWithUserGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: Feedback)

    @Query("SELECT * FROM feedbacks ORDER BY timestamp DESC")
    fun getAllFeedback(): Flow<List<Feedback>>

    @Query("""
    SELECT f.feedbackId, f.message, f.timestamp,
           u.firstName || ' ' || u.lastName AS userName,
           g.groupName AS groupName
    FROM feedbacks f
    INNER JOIN users u ON f.userId = u.userId
    LEFT JOIN group_members gm ON u.userId = gm.userId
    LEFT JOIN 'groups' g ON gm.groupId = g.groupId
    ORDER BY f.timestamp DESC
""")
    fun getFeedbackWithUserGroup(): Flow<List<FeedbackWithUserGroup>>

}