package com.mynikatech.apnafund.lambda.email

object FundClosureEmail {

    fun subject(
        fundName: String
    ): String =
        "Fund Closed: $fundName | Apna Fund"


    fun body(
        userName: String,
        fundName: String,
        closedByName: String,
        reason: String
    ): String {

        return """
            <p>
                The fund <strong>$fundName</strong> has been successfully 
                <strong>closed</strong> on <strong>Apna Fund</strong>.
            </p>

            <p>
                <strong>Closed By:</strong> $closedByName<br/>
                <strong>Reason for Closure:</strong><br/>
                $reason
            </p>

            <p>
                You can log in to the app to view final fund details,
                contribution history, and completed transactions.
            </p>

            <p>
                Thank you for being part of Apna Fund.
            </p>

            <br/>
        """.trimIndent()
    }
}