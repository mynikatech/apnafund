package com.mynikatech.apnafund.server.security

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery

interface PinHistorySql {

    // CREATE -> returns inserted id
    @SqlQuery("""SELECT add_user_pin_history(:userId, :pinHash)""")
    fun insertPinHistory(@Bind("userId") userId: Int,
                         @Bind("pinHash") pinHash: String): Int

    // READ last 3 pin hashes (newest first)
    @SqlQuery("""SELECT * FROM get_last3_pin_hashes(:userId)""")
    fun getLast3PinHashes(@Bind("userId") userId: Int): List<String>

    // READ last change timestamp (epoch millis)
    @SqlQuery("""SELECT get_last_pin_change_ms(:userId)""")
    fun getLastPinChangeMs(@Bind("userId") userId: Int): Long?
}
