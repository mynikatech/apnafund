package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.data.model.Feedback
import com.mynikatech.apnafund.net.dto.FeedbackWithUserGroupDto
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminViewModel : ViewModel() {

    private val feedbackRepository = ApnaFundApplication.feedbackRepository

    val feedbackWithUserGroup: Flow<List<FeedbackWithUserGroupDto>> = feedbackRepository.getFeedbackWithUserGroup()

    suspend fun submitFeedback(userId: Int, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        // save it on the server
        feedbackRepository.saveFeedback(
            Feedback(
                userId = userId,
                message = message,
                timestamp = timestamp
            )
        )
    }
}