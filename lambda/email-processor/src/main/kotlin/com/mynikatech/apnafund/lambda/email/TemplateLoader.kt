package com.mynikatech.apnafund.lambda.email

object TemplateLoader {

    fun loadHtml(fileName: String): String =
        TemplateLoader::class.java
            .classLoader
            ?.getResourceAsStream("templates/$fileName")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("Email template not found: $fileName")
}