package com.mynikatech.apnafund.lambda.email

object LoanRejectedEmail {

    fun subject(): String {
        return "Loan Request Rejected | Apna Fund"
    }

    fun body(
        userName: String,
        fundName: String,
        rejectedBy: String,
        reason: String,
        loanId: String,
        loanAmount: String,
        issueDate: String
    ): String {

        return """
        <p>Dear <strong>$userName</strong>,</p>

        <p>
            Your loan request in the fund <strong>$fundName</strong> has been 
            <strong style="color:red;">REJECTED</strong>.
        </p>

        <p><strong>Loan Details</strong></p>
        <ul>
            <li><strong>Loan Reference:</strong> LN-$loanId</li>
            <li><strong>Loan Amount:</strong> ₹$loanAmount</li>
            <li><strong>Requested Issue Date:</strong> $issueDate</li>
        </ul>

        <p><strong>Reviewed by:</strong> $rejectedBy</p>

        <p><strong>Reason for rejection:</strong><br/>
        $reason
        </p>

        <p>
            You may contact the moderator for further clarification.
        </p>

        <br/>
        <p>Regards,<br/>ApnaFund Team</p>
        """.trimIndent()
    }
}