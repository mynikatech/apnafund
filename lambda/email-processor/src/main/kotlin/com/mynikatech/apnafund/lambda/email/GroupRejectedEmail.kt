package com.mynikatech.apnafund.lambda.email

object GroupRejectedEmail {

    fun subject(groupName: String) =
        "Group Request Update | Apna Fund"

    fun body(
        userName: String,
        groupName: String
    ) = """
        <p>
            Your request to create the group
            <strong>$groupName</strong>
            was not approved at this time.
        </p>

        <p>
            Please contact support for more details
            or submit a revised request.
        </p>

        <br/>
    """.trimIndent()
}