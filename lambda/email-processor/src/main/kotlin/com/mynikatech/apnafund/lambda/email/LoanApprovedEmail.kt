package com.mynikatech.apnafund.lambda.email

object LoanApprovedEmail {

    fun subject(): String =
        "Loan Approved | Apna Fund"

    fun body(
        userName: String,
        fundName: String,
        approvedBy: String,
        loanId: String,
        loanAmount: String,
        issueDate: String,
        maturityDate: String
    ): String {

        return """
            <p>Dear <strong>$userName</strong>,</p>

            <p>
                Your loan request in 
                <strong>$fundName</strong> has been 
                <strong style="color:green;">APPROVED</strong>.
            </p>

            <p><strong>Loan Details</strong></p>
            <ul>
                <li><strong>Loan Reference:</strong> LN-$loanId</li>
                <li><strong>Loan Amount:</strong> ₹$loanAmount</li>
                <li><strong>Issue Date:</strong> $issueDate</li>
                <li><strong>Maturity Date:</strong> $maturityDate</li>
            </ul>

            <p>
                Approved by: <strong>$approvedBy</strong>
            </p>

            <p>
                EMI schedule and loan details are now available in the app.
            </p>

            <br/>
            <p>Regards,<br/>ApnaFund Team</p>
        """.trimIndent()
    }
}