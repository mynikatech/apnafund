package com.mynikatech.apnafund.lambda.email

import com.mynikatech.apnafund.net.dto.SupportEvent

object SupportEmailRenderer {

    fun render(event: SupportEvent): Pair<String, String> {
        val template = load("support-base.html")

        val subject = "[${event.source}] ${event.subject}"

        val body = template
            .replace("{{SOURCE}}", event.source.name)
            .replace("{{USER_EMAIL}}", event.userEmail ?: "N/A")
            .replace("{{MESSAGE_BODY}}", event.message)
            .replace("{{CREATED_AT}}", event.createdAt.toString())

        return subject to body
    }

    private fun load(name: String): String =
        object {}.javaClass
            .getResource("/templates/$name")
            ?.readText()
            ?: error("Template not found: $name")
}
