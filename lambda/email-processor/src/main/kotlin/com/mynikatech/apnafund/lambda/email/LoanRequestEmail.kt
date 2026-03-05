package com.mynikatech.apnafund.lambda.email

object LoanRequestEmail {

    fun subject(): String =
        "Loan Approval Required | Apna Fund"

    fun body(
        userName: String,
        borrowerName: String,
        fundName: String,
        loanId: String,
        loanAmount: String,
        issueDate: String,
        loanPeriod: String
    ): String {

        return """
            <p>Dear <strong>$userName</strong>,</p>

            <p>
                A loan request has been submitted in 
                <strong>$fundName</strong>.
            </p>

            <p><strong>Loan Details</strong></p>
            <ul>
                <li><strong>Borrower:</strong> $borrowerName</li>
                <li><strong>Loan Reference:</strong> LN-$loanId</li>
                <li><strong>Loan Amount:</strong> ₹$loanAmount</li>
                <li><strong>Requested Issue Date:</strong> $issueDate</li>
                <li><strong>Loan Period:</strong> $loanPeriod months</li>
            </ul>

            <p>
                Please log in to <strong>Apna Fund</strong> 
                to approve or reject this request.
            </p>

            <br/>
        """.trimIndent()
    }
}