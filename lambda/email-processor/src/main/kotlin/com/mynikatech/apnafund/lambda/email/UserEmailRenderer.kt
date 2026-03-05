package com.mynikatech.apnafund.lambda.email

import com.mynikatech.apnafund.net.dto.NotificationEvent

object UserEmailRenderer {

    fun render(event: NotificationEvent): Pair<String, String> {
        val template = load("apnafund-base.html")
        val content = UserNotificationContentRegistry.get(event)
        val email = event.email
            ?: error("EmailPayload missing for EMAIL channel")
        val body = template
            .replace("{{USER_NAME}}", email.userName)
            .replace("{{MESSAGE_BODY}}", content.message(event))

        return content.subject to body
    }

    private fun load(name: String): String =
        object {}.javaClass
            .getResource("/templates/$name")
            ?.readText()
            ?: error("Template not found: $name")
}
