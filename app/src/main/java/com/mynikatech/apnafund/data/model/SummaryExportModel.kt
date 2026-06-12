package com.mynikatech.apnafund.data.model

data class SummaryExportModel(
    val title: String,
    val subTitle: String?,
    val headers: List<String>,
    val rows: List<SummaryExportRow>
)
