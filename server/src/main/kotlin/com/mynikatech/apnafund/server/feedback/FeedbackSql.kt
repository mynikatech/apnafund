package com.mynikatech.apnafund.server.feedback

import com.mynikatech.apnafund.net.dto.FeedbackDto
import com.mynikatech.apnafund.net.dto.FeedbackWithUserGroupDto
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

@RegisterKotlinMapper(FeedbackDto::class)
@RegisterKotlinMapper(FeedbackWithUserGroupDto::class)
interface FeedbackSql {
    @SqlUpdate("""
        INSERT INTO feedbacks("userId","message","timestamp")
        VALUES(:userId,:message,:timestamp)
    """)
    fun insertFeedback(@BindKotlin f: FeedbackDto): Int

    @SqlQuery("""SELECT * FROM get_feedback_with_user_group()""")
    fun getAllFeedbacksWithUserGroup(): List<FeedbackWithUserGroupDto>

    @SqlQuery("""SELECT "feedbackId","userId","message","timestamp" FROM feedbacks ORDER BY "timestamp" DESC""")
    fun getAllFeedbacks(): List<FeedbackDto>
}
