package com.mynikatech.apnafund.data.repository

import com.mynikatech.apnafund.data.dao.FeedbackDao
import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.data.model.Feedback
import com.mynikatech.apnafund.net.FeedbackApi
import com.mynikatech.apnafund.net.dto.FeedbackDto
import com.mynikatech.apnafund.net.dto.FeedbackWithUserGroupDto
import kotlinx.coroutines.flow.Flow

class FeedbackRepository(
    private val feedbackDao: FeedbackDao,
    private val feedbackApi: FeedbackApi
) {
    suspend fun saveFeedback(feedback: Feedback) {
        val remoteSuccess = feedbackApi.insertFeedback(feedback.toDto())
        //if (remoteSuccess)
            //feedbackDao.insertFeedback(feedback)
    }

    fun getAllFeedback(): Flow<List<FeedbackDto>> {
        return  kotlinx.coroutines.flow.flow { emit(feedbackApi.getAllFeedbacks()) }
    }

    fun getFeedbackWithUserGroup(): Flow<List<FeedbackWithUserGroupDto>> =
        kotlinx.coroutines.flow.flow { emit(feedbackApi.getAllFeedbacksWithUserGroup()) }
}