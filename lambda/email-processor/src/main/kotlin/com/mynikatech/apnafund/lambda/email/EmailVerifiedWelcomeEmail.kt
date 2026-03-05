package com.mynikatech.apnafund.lambda.email

object EmailVerifiedWelcomeEmail {

    fun subject(): String =
        "Welcome to ApnaFund"

    fun body(
        userName: String
    ): String {
        return  """
        <p>Your email has been successfully verified.</p>

        <p>
            Welcome to <strong>ApnaFund</strong>!
            You can now fully access your account.
        </p>

        """.trimIndent()
    }
}