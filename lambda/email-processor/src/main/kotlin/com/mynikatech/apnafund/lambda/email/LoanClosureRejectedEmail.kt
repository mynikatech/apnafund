package com.mynikatech.apnafund.lambda.email

object LoanClosureRejectedEmail {

    fun subject(
        fundName: String
    ): String =
        "Loan Closure Request Rejected | $fundName | Apna Fund"


    fun body(
        userName: String,
        fundName: String,
        loanAmount: String,
        rejectedBy: String,
        reason: String,
        loanNumber: String? = null
    ): String {

        val loanNumberHtml = loanNumber?.let {
            "<strong>Loan No:</strong> $it<br/>"
        } ?: ""

        val reasonHtml = if (reason.isNotBlank()) {
            """
            <p>
                <strong>Reason for Rejection:</strong><br/>
                $reason
            </p>
            """.trimIndent()
        } else ""

        return """
            <p>
                Hello <strong>$userName</strong>,
            </p>

            <p>
                Your request to close the loan of 
                <strong>₹$loanAmount</strong> in fund 
                <strong>$fundName</strong> has been 
                <strong>rejected</strong>.
            </p>

            <p>
                $loanNumberHtml
                <strong>Reviewed By:</strong> $rejectedBy
            </p>

            $reasonHtml

            <p>
                You may continue your regular EMI payments as scheduled.
                If you have any questions, please contact your fund moderator.
            </p>

            <p>
                Thank you for using <strong>Apna Fund</strong>.
            </p>

            <br/>
        """.trimIndent()
    }
}