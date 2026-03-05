object AdminGroupPendingApprovalEmail {

    fun subject(): String =
        "New Group Approval Request – ApnaFund"

    fun body(
        moderatorName: String,
        groupName: String
    ): String {
        return """
            <p>
                A new group <strong>$groupName</strong> has been created by
                <strong>$moderatorName</strong> and is pending approval.
            </p>

            <p>
                Please review and approve the group from the Admin section of the app.
            </p>
        """.trimIndent()
    }
}