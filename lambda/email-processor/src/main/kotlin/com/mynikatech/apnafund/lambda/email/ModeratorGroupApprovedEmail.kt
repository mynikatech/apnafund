object ModeratorGroupApprovedEmail {

    fun subject(): String =
        "Your Group Has Been Approved 🎉"

    fun body(
        moderatorName: String,
        groupName: String
    ): String {
        return """
            <p>
                Your group <strong>$groupName</strong> has been approved successfully.
            </p>

            <p>
                You can now start creating funds and adding members.
            </p>
        """.trimIndent()
    }
}
