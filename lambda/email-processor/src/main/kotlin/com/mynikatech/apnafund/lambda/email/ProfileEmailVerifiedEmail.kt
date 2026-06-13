package com.mynikatech.apnafund.lambda.email

object ProfileEmailVerifiedEmail {

    fun subject(): String =
        "Email Address Verified"

    fun body(
        userName: String
    ): String {
        return """
        <p>Hello $userName,</p>

        <p>
            Your email address has been successfully verified and is now active for your
            <strong>ApnaFund</strong> account.
        </p>

        <p>
            If you recently updated your email address, no further action is required.
        </p>

        <p>
            If you did not perform this action, please contact support immediately.
        </p>
        """.trimIndent()
    }
}