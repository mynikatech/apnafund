package com.mynikatech.apnafund.lambda.email

object GroupApprovedEmail {

    fun subject(groupName: String) =
        "Group Approved: $groupName | Apna Fund"

    fun body(
        userName: String,
        groupName: String
    ) = """
        <p>Hi $userName,</p>

        <p>
            Congratulations! Your group
            <strong>$groupName</strong>
            has been successfully approved on
            <strong>Apna Fund</strong>.
        </p>

        <p>
            You can now start creating funds,
            adding members, and managing contributions.
        </p>

        <p>Welcome aboard!</p>

        <br/>
        Regards,<br/>
        Apna Fund Team
    """.trimIndent()
}