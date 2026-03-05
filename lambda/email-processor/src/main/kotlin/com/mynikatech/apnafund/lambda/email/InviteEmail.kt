package com.mynikatech.apnafund.lambda.email

object InviteEmail {

    fun subject(
        groupName: String
    ): String =
        "\"You’re invited to join $groupName on Apna Fund\""

    fun body(
        userName: String,
        invitedBy: String,
        groupName: String
    ): String {
        return """
            <p>
            <strong>${invitedBy}</strong> has invited you to join the 
            <strong>${groupName}</strong> group on <strong>Apna Fund</strong>.
        </p>

        <p>
            To get started:
            <ol>
                <li>Download the <strong>Apna Fund</strong> app from the Play Store</li>
                <li>Login using this email address</li>
                <li>Set your password and verify your email</li>
            </ol>
        </p>

        <p>
            If you were not expecting this invitation, you can safely ignore this email.
        </p>

        """.trimIndent()
    }
}
