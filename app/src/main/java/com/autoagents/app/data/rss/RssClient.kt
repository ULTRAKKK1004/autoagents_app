package com.autoagents.app.data.rss

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

data class RssItem(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val source: String,
    val imageUrl: String?
)

data class RssSource(val name: String, val url: String)

class RssClient {

    suspend fun fetch(source: RssSource): List<RssItem> = withContext(Dispatchers.IO) {
        val xml = try {
            Jsoup.connect(source.url)
                .userAgent("Mozilla/5.0 AutoAgents/1.0")
                .timeout(15_000)
                .ignoreContentType(true)
                .execute()
                .body()
        } catch (_: Throwable) {
            return@withContext emptyList<RssItem>()
        }
        parse(xml, source.name)
    }

    suspend fun fetchAll(sources: List<RssSource>, perSource: Int = 5): List<RssItem> =
        coroutineScope {
            sources.map { src ->
                async { fetch(src).take(perSource) }
            }.awaitAll().flatten().sortedByDescending { it.pubDate }
        }

    private fun parse(xml: String, sourceName: String): List<RssItem> {
        if (xml.isBlank()) return emptyList()
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        val items = mutableListOf<RssItem>()

        // RSS 2.0
        doc.select("item").forEach { it ->
            val title = it.selectFirst("title")?.text()?.trim().orEmpty()
            val link = it.selectFirst("link")?.text()?.trim()
                ?: it.selectFirst("guid")?.text()?.trim().orEmpty()
            val desc = it.selectFirst("description")?.text()?.trim().orEmpty()
            val pubDate = it.selectFirst("pubDate")?.text()?.trim().orEmpty()
            val image = extractImage(it.outerHtml(), desc)
            if (title.isNotBlank() && link.isNotBlank()) {
                items += RssItem(title, link, desc, pubDate, sourceName, image)
            }
        }

        // Atom 1.0
        doc.select("entry").forEach { it ->
            val title = it.selectFirst("title")?.text()?.trim().orEmpty()
            val link = it.selectFirst("link")?.attr("href")?.trim().orEmpty()
            val desc = it.selectFirst("summary")?.text()?.trim()
                ?: it.selectFirst("content")?.text()?.trim().orEmpty()
            val pubDate = it.selectFirst("updated")?.text()?.trim()
                ?: it.selectFirst("published")?.text()?.trim().orEmpty()
            val image = extractImage(it.outerHtml(), desc)
            if (title.isNotBlank() && link.isNotBlank()) {
                items += RssItem(title, link, desc, pubDate, sourceName, image)
            }
        }
        return items
    }

    private fun extractImage(rawHtml: String, fallback: String): String? {
        val mediaPattern = Regex("""url=["']([^"']+\.(?:jpg|jpeg|png|webp|gif))["']""", RegexOption.IGNORE_CASE)
        mediaPattern.find(rawHtml)?.groupValues?.getOrNull(1)?.let { return it }
        val imgPattern = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        imgPattern.find(rawHtml)?.groupValues?.getOrNull(1)?.let { return it }
        imgPattern.find(fallback)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }

    companion object {
        val DEFAULT_SOURCES: List<RssSource> = listOf(
            RssSource("GeekNews", "https://news.hada.io/rss"),
            RssSource("PyTorch Korea", "https://pytorch.kr/feed.xml"),
            RssSource("Meeco News", "https://meeco.kr/newsrss"),
            RssSource("TechCrunch", "https://feeds.feedburner.com/TechCrunch/"),
            RssSource("The Verge", "https://www.theverge.com/rss/index.xml"),
            RssSource("Wired", "https://www.wired.com/feed/rss"),
            RssSource("Ars Technica", "https://arstechnica.com/feed/"),
            RssSource("Engadget", "https://www.engadget.com/rss.xml"),
            RssSource("9to5Mac", "https://9to5mac.com/feed/"),
            RssSource("BBC Tech", "https://feeds.bbci.co.uk/news/technology/rss.xml"),
            RssSource("ZDNet", "https://www.zdnet.com/news/rss.xml"),
            RssSource("CNET", "https://www.cnet.com/rss/news/")
        )
    }
}
