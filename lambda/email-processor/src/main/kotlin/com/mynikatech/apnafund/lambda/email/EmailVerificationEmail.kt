package com.mynikatech.apnafund.lambda.email

object EmailVerificationEmail {

    fun subject(): String =
        "Verify your email for ApnaFund"

    fun body(
        userName: String,
        otp: String
    ): String {
        return """
            <p>
                Your email verification code for <strong>ApnaFund</strong> is:
            </p>

            <h2 style="letter-spacing: 3px;">$otp</h2>

            <p>
                This code is valid for <strong>1 hour</strong>.
                Please enter it in the app to verify your email address.
            </p>

            <p>
                If you did not request this, you can safely ignore this email.
            </p>
        """.trimIndent()
    }
}
