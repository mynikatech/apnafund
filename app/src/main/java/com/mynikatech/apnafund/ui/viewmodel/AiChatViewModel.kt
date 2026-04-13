package com.mynikatech.apnafund.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.net.dto.AIChatMessage
import com.mynikatech.apnafund.net.dto.AIContext
import com.mynikatech.apnafund.net.dto.AIRequest
import com.mynikatech.apnafund.net.dto.AIResponseType
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.util.replaceThinking
import kotlinx.coroutines.launch

class AiChatViewModel(
) : ViewModel() {
    private val repository = ApnaFundApplication.aiRepository

    private val _messages = MutableLiveData<List<AIChatMessage>>(emptyList())
    val messages: LiveData<List<AIChatMessage>> = _messages

    // 🔹 Context stored inside ViewModel
    private var userId: Int = 0
    private var groupId: Int? = null
    private var fundId: Int? = null

    fun setContext(userId: Int, groupId: Int?, fundId: Int?) {
        this.userId = userId
        this.groupId = groupId
        this.fundId = fundId
    }

    fun sendMessage(userMessage: String) {

        val current = _messages.value?.toMutableList() ?: mutableListOf()

        // Add user message
        current.add(AIChatMessage(userMessage, true))

        // Add thinking
        current.add(AIChatMessage("Thinking...", false))

        _messages.value = current

        viewModelScope.launch {

            val updated = _messages.value?.toMutableList() ?: mutableListOf()

            try {
                val response = repository.query(
                    AIRequest(
                        message = userMessage,
                        context = AIContext(
                            userId = SessionManager.userId
                        )
                    )
                )

                updated.replaceThinking(
                    AIChatMessage(
                        message = response.reply,
                        isUser = false,
                        type = response.type,
                        data = response.data,
                        actions = response.actions
                    )
                )

            } catch (e: Exception) {
                Log.e("AiChatViewModel", e.message.toString())
                updated.replaceThinking(
                    AIChatMessage(
                        message = "Unable to process request. Please try again.",
                        isUser = false,
                        type = AIResponseType.ERROR
                    )
                )
            }

            _messages.postValue(updated)
        }
    }
}