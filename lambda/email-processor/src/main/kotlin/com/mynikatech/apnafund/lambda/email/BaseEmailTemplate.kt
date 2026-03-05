import com.mynikatech.apnafund.lambda.email.TemplateLoader.loadHtml

object BaseEmailTemplate {

    fun wrap(
        userName: String,
        messageBodyHtml: String
    ): String {
        val baseHtml = loadHtml("apnafund-base.html")

        return baseHtml
            .replace("{{USER_NAME}}", userName)
            .replace("{{MESSAGE_BODY}}", messageBodyHtml)
    }
}
