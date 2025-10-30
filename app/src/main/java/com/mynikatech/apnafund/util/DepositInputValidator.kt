package com.mynikatech.apnafund.util


object DepositInputValidator {

    fun isPrepayMultipleOf(amount: Double, multiple: Int): Boolean {
        return amount % multiple == 0.0
    }



}