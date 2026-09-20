package com.mynikatech.apnafund.lambda.email

interface EmailSender {
    fun send(
        to: String,
        subject: String,
        body: String
    )
}