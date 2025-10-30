package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.Table

object FeedbacksTable : Table(name = "feedbacks") {
    val feedbackId = integer("feedbackId").autoIncrement()
    val userId     = integer("userId")            // Room model has no FK; keep as INT
    val message    = text("message")
    val timestamp  = text("timestamp")            // keep as TEXT to match Room String

    override val primaryKey = PrimaryKey(feedbackId)

    init {
        index(isUnique = false, columns = arrayOf(userId))
    }
}
