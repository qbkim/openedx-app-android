package com.raccoongang.core.extension

import android.os.Parcelable
import android.util.Patterns
import com.raccoongang.core.BuildConfig
import kotlinx.parcelize.Parcelize
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

object TextConverter {

    fun htmlTextToLinkedText(html: String): LinkedText {
        val doc: Document =
            Jsoup.parse(html)
        val links: Elements = doc.select("a[href]")
        val text = doc.text()
        val linksMap = mutableMapOf<String, String>()
        for (link in links) {
            val resultLink = if (link.attr("href").isNotEmpty() && link.attr("href")[0] == '/') {
                link.attr("href").substring(1)
            } else {
                link.attr("href")
            }
            if (resultLink.isNotEmpty() && isLinkValid(BuildConfig.BASE_URL + resultLink)) {
                linksMap[link.text()] = BuildConfig.BASE_URL + resultLink
            }
        }
        return LinkedText(text, linksMap.toMap())
    }

    fun textToLinkedImageText(html: String): LinkedImageText {
        val doc: Document =
            Jsoup.parse(html)
        val links: Elements = doc.select("a[href]")
        var text = doc.text()
        val headers = getHeaders(doc)
        val linksMap = mutableMapOf<String, String>()
        for (link in links) {
            if (isLinkValid(link.attr("href"))) {
                val linkText = if (link.hasText()) link.text() else link.attr("href")
                linksMap[linkText] = link.attr("href")
            } else {
                val resultLink =
                    if (link.attr("href").isNotEmpty() && link.attr("href")[0] == '/') {
                        link.attr("href").substring(1)
                    } else {
                        link.attr("href")
                    }
                if (resultLink.isNotEmpty() && isLinkValid(BuildConfig.BASE_URL + resultLink)) {
                    linksMap[link.text()] = BuildConfig.BASE_URL + resultLink
                }
            }
        }
        text = setSpacesForHeaders(text, headers)
        return LinkedImageText(
            text,
            linksMap.toMap(),
            getImageLinks(doc),
            headers
        )
    }

    fun isLinkValid(link: String) = Patterns.WEB_URL.matcher(link.lowercase()).matches()

    private fun getHeaders(document: Document): List<String> {
        val headersList = mutableListOf<String>()
        for (index in 1..6) {
            if (document.select("h$index").hasText()) {
                headersList.add(document.select("h$index").text())
            }
        }
        return headersList.toList()
    }

    private fun setSpacesForHeaders(text: String, headers: List<String>): String {
        var result = text
        headers.forEach {
            val startIndex = text.indexOf(it)
            val endIndex = startIndex + it.length + 1
            result = text.replaceRange(startIndex, endIndex, it + "\n")
        }
        return result
    }

    private fun getImageLinks(document: Document): Map<String, String> {
        val imageLinks = mutableMapOf<String, String>()
        val elements = document.getElementsByTag("img")
        for (element in elements) {
            if (element.hasAttr("alt")) {
                imageLinks[element.attr("alt")] = element.attr("src")
            } else {
                imageLinks[element.attr("src")] = element.attr("src")
            }
        }
        return imageLinks.toMap()
    }
}

data class LinkedText(
    val text: String,
    val links: Map<String, String>
)

@Parcelize
data class LinkedImageText(
    val text: String,
    val links: Map<String, String>,
    val imageLinks: Map<String, String>,
    val headers: List<String>
) : Parcelable
