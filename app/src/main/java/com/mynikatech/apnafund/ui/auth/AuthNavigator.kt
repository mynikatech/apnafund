package com.mynikatech.apnafund.ui.auth

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.session.SessionManager

object AuthNavigator {

    fun exitAuthFlow(navController: NavController) {
        val destinationId =
            if (SessionManager.isPinSet)
                R.id.loginPinFragment
            else
                R.id.loginFragment

        navController.navigate(
            destinationId,
            NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
        )
    }
}