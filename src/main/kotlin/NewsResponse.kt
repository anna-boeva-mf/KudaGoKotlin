package ru.tbank

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import ru.tbank.dto.News
import ru.tbank.dto.toHumanDate
import kotlin.collections.emptyList
import java.time.LocalDate
import java.io.File

private val logger = LoggerFactory.getLogger(::NewsResponse.javaClass)

var URL: String = "https://kudago.com/public-api/v1.4/news/"

@Serializable
data class NewsResponse(val results: List<News>)


suspend fun getNews(count: Int = 100): List<News> {
    logger.info("Формируем запрос к сервису")
    val client = HttpClient()
    val json = Json { ignoreUnknownKeys = true }
    try {
        val response: HttpResponse = client.get(URL) {
            contentType(ContentType.Application.Json)
            parameter("location", "msk")
            parameter(
                "fields",
                "id,title,place,description,site_url,favorites_count,comments_count,publication_date"
            )
            parameter("order_by", "-publication_date")
            parameter("page_size", count)
        }
        val newsResponse = json.decodeFromString<NewsResponse>(response.bodyAsText())
        logger.info("Результат запроса получен")
        return newsResponse.results
    } catch (e: Exception) {
        logger.error("Ошибка получения новостей: ${e.message}")
        return emptyList()
    } finally {
        client.close()
    }
}

fun List<News>.getMostRatedNews(count: Int, period: ClosedRange<LocalDate>): List<News> {
    logger.info("Получаем все новости за указанный промежуток времени, после чего сортируем их по возрастанию по рейтингу")
    return this.asSequence()
        .filter { it.publicationDate?.toHumanDate()!! in period }
        .sortedByDescending { it.rating }
        .take(count)
        .toList()
}

fun saveNews(path: String, news: Collection<News>) {
    logger.info("Сохраняем новости в файл по указанному пути: $path")
    require(File(path).parentFile.exists() || File(path).parentFile.mkdirs()) {
        "Невозможно создать папку или файл: $path"
    }
    require(!File(path).exists()) {
        "Файл уже существует по пути: $path"
    }
    val out = File(path).printWriter()
    try {
        out.println("id,title,place,description,siteUrl,favoritesCount,commentsCount,publicationDate")
        news.forEach { news ->
            out.printf(
                "%d,%s,%s,%s,%s,%d,%d,%s\n",
                news.id, news.title, news.place, news.description, news.siteUrl, news.favoritesCount, news.commentsCount,
                news.publicationDate?.toHumanDate()
            )
        }
        logger.info("Файл сохранен")
    } finally {
        out.close()
    }
}
