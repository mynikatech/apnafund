package com.mynikatech.apnafund.lambda.email

object LoanClosureRequestEmail {

    fun subject(
        fundName: String
    ): String =
        "Loan Closure Request Pending | $fundName | Apna Fund"


    fun body(
        userName: String,          // moderator name
        fundName: String,
        borrowerName: String,
        loanAmount: String,
        loanNumber: String? = null,
        issuedDate: String? = null,
        maturityDate: String? = null,
        requestedAmount: String? = null,
        remarks: String? = null
    ): String {

        val loanNumberHtml = loanNumber?.let {
            "<strong>Loan No:</strong> $it<br/>"
        } ?: ""

        val issuedDateHtml = issuedDate?.let {
            "<strong>Issued On:</strong> $it<br/>"
        } ?: ""

        val maturityDateHtml = maturityDate?.let {
            "<strong>Maturity Date:</strong> $it<br/>"
        } ?: ""

        val requestedAmountHtml = requestedAmount?.let {
            "<strong>Requested Closure Amount (Approx):</strong> ₹$it<br/>"
        } ?: ""

        val remarksHtml = remarks?.takeIf { it.isNotBlank() }?.let {
            "<strong>Remarks:</strong> $it<br/>"
        } ?: ""

        return """
            <p>
                Hello <strong>$userName</strong>,
            </p>

            <p>
                <strong>$borrowerName</strong> has requested to 
                <strong>close their loan</strong> in fund 
                <strong>$fundName</strong>.
            </p>

            <p>
                <strong>Loan Amount:</strong> ₹$loanAmount<br/>
                $loanNumberHtml
                $issuedDateHtml
                $maturityDateHtml
                $requestedAmountHtml
                $remarksHtml
            </p>

            <p>
                Please review this request and take appropriate action.
            </p>

            <p>
                You can log in to <strong>Apna Fund</strong> to approve or reject this request.
            </p>

            <br/>
        """.trimIndent()
    }
}