package com.mynikatech.apnafund.server.security

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery

interface PasswordHistorySql {

    // CREATE -> returns inserted id
    @SqlQuery("""SELECT add_user_password_history(:userId, :passwordHash)""")
    fun insertPasswordHistory(@Bind("userId") userId: Int,
                              @Bind("passwordHash") passwordHash: String): Int

    // READ last 3 password hashes (newest first)
    @SqlQuery("""SELECT * FROM get_last3_password_hashes(:userId)""")
    fun getLast3PasswordHashes(@Bind("userId") userId: Int): List<String>

    // READ last change timestamp (epoch millis)
    @SqlQuery("""SELECT get_last_password_change_ms(:userId)""")
    fun getLastPasswordChangeMs(@Bind("userId") userId: Int): Long?
}
