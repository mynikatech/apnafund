package com.mynikatech.apnafund.lambda.email

object LoanClosureEmail {

    fun subject(
        fundName: String
    ): String =
        "Loan Closed in $fundName | Apna Fund"


    fun body(
        userName: String,
        fundName: String,
        borrowerName: String,
        loanAmount: String,
        closureType: String,
        closedByName: String,
        loanNumber: String? = null,
        closedDate: String? = null
    ): String {

        val loanNumberHtml = loanNumber?.let {
            "<strong>Loan No:</strong> $it<br/>"
        } ?: ""

        val closedDateHtml = closedDate?.let {
            "<strong>Closed On:</strong> $it<br/>"
        } ?: ""

        val displayClosureType = when (closureType.uppercase()) {
            "FORECLOSURE" -> "Foreclosure (Early Closure)"
            "MATURITY" -> "Completed as per schedule"
            else -> closureType
        }

        return """
            <p>
                Hello <strong>$userName</strong>,
            </p>

            <p>
                The loan of <strong>₹$loanAmount</strong> for 
                <strong>$borrowerName</strong> in fund 
                <strong>$fundName</strong> has been successfully 
                <strong>closed</strong>.
            </p>

            <p>
                $loanNumberHtml
                $closedDateHtml
                <strong>Closure Type:</strong> $displayClosureType<br/>
                <strong>Closed By:</strong> $closedByName
            </p>

            <p>
                You can log in to the app to view complete loan details,
                repayment history, and final settlement.
            </p>

            <p>
                Thank you for using <strong>Apna Fund</strong>.
            </p>

            <br/>
        """.trimIndent()
    }
}