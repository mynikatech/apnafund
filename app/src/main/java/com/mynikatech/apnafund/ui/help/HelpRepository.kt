package com.mynikatech.apnafund.ui.help

import android.content.Context
import com.mynikatech.apnafund.R

object HelpRepository {

    fun getSections(): List<HelpSection> {
        return listOf(

            HelpSection(
                id = "deposit",
                titleResId = R.string.section_help_deposit,
                category = HelpCategory.DEPOSIT,
                items = listOf(
                    HelpItem(
                        id = "deposit_add",
                        titleResId = R.string.help_deposit_add_title,
                        descriptionResId = R.string.help_deposit_add_desc,
                        deepLink = R.id.fundDepositsFragment,
                        videoUrl = "https://your-video-link"
                    )
                )
            ),
            HelpSection(
                id = "group",
                titleResId = R.string.section_help_group,
                category = HelpCategory.GROUP,
                items = listOf(
                    HelpItem(
                        id = "group_general",
                        titleResId = R.string.help_title_general_description,
                        descriptionResId = R.string.help_group_general_desc,
                        deepLink = R.id.groupFragment,
                        videoUrl = "https://your-video-link"
                    )
                )
            ),
            HelpSection(
                id = "fund",
                titleResId = R.string.section_help_fund,
                category = HelpCategory.FUND,
                items = listOf(
                    HelpItem(
                        id = "fund_general",
                        titleResId = R.string.help_title_general_description,
                        descriptionResId = R.string.help_fund_general_desc,
                        deepLink = R.id.groupFragment,
                        videoUrl = "https://your-video-link"
                    )
                )
            ),
            HelpSection(
                id = "loans",
                titleResId = R.string.section_help_loan,
                category = HelpCategory.LOAN,
                items = listOf(
                    HelpItem(
                        id = "emi_add",
                        titleResId = R.string.help_loan_emi_add_title,
                        descriptionResId = R.string.help_loan_emi_add_desc,
                        deepLink = R.id.groupFragment,
                        videoUrl = "https://your-video-link"
                    )
                )
            )

        )
    }
    fun search(query: String, context: Context): List<HelpItem> {
        return getSections()
            .flatMap { it.items }
            .filter {
                context.getString(it.titleResId).contains(query, true) ||
                        context.getString(it.descriptionResId).contains(query, true)
            }
    }
}