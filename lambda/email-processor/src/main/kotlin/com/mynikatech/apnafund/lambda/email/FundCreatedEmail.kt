package com.mynikatech.apnafund.lambda.email

object FundCreatedEmail {

    fun subject(
        fundName: String
    ): String =
        "\"New Fund Created: $fundName on Apna Fund\""

    fun body(
        userName: String,
        fundName: String,
        fundMembers: String
    ): String {

        return """
            <p>
                A new fund <strong>"$fundName"</strong> has been successfully created in your group on 
                <strong>Apna Fund</strong>.
            </p>

            <p>
                <strong>Fund Members:</strong><br/>
                $fundMembers
            </p>

            <p>
                You can now log in to the app to view fund details, track contributions, 
                and stay updated with transactions.
            </p>

           """.trimIndent()
    }
}
