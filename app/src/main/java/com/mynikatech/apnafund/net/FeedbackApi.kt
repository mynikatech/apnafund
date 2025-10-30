package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.FeedbackDto
import com.mynikatech.apnafund.net.dto.FeedbackWithUserGroupDto

interface FeedbackApi {

    // feedback
    suspend fun getAllFeedbacks(): List<FeedbackDto>
    suspend fun getAllFeedbacksWithUserGroup(): List<FeedbackWithUserGroupDto>
    suspend fun insertFeedback(feedback: FeedbackDto): Boolean
}