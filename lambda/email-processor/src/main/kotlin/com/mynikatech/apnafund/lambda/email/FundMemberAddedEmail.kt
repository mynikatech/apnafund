package com.mynikatech.apnafund.lambda.email

object FundMemberAddedEmail {

    fun subject(): String =
        "You’ve Been Added to a Fund 🎉"

    fun body(
        userName: String,
        fundName: String
    ): String {

        return """
            <p>
                You have been successfully added to the fund 
                <strong>"$fundName"</strong> on <strong>Apna Fund</strong>.
            </p>

            <p>
                You can now log in to the app to view fund details, 
                track contributions, and stay updated.
            </p>

        """.trimIndent()
    }
}
