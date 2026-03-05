package com.mynikatech.apnafund.lambda.email

object ModeratorGroupRejectedEmail {

    fun subject(): String =
        "Your Group Approval Request Was Rejected"

    fun body(
        moderatorName: String,
        groupName: String,
        reason: String? = null
    ): String {
        val reasonBlock = reason?.let {
            """
            <p>
                <strong>Reason:</strong> $it
            </p>
            """.trimIndent()
        } ?: ""

        return """
            <p>
                Unfortunately, your group <strong>$groupName</strong> was not approved.
            </p>

            $reasonBlock

            <p>
                You may contact support if you believe this was a mistake.
            </p>
        """.trimIndent()
    }
}
