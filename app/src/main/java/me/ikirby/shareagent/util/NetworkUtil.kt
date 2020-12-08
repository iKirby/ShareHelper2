package me.ikirby.shareagent.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36"

@Throws(IOException::class)
private fun getDocument(url: String): Document {
    val headers = mapOf(
        "User-Agent" to USER_AGENT
    )
    return Jsoup.connect(url)
        .headers(headers)
        .timeout(10000)
        .get()
}

@Throws(IOException::class)
fun getTitleFromHtml(url: String): String {
    val document = getDocument(url)
    return document.title()
}
